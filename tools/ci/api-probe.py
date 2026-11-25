#!/usr/bin/env python3
"""Temporary CI probe for the Minecraft 26.2 port.

Collects hard evidence about the new API surface instead of guessing:

* the jars Gradle actually resolved (plus every jar found in the Gradle cache),
* the imports used by the mod sources,
* whether each imported fully qualified class name still exists, and
* ``javap`` output for the classes whose 26.2 shape matters.

The full report is written to ``build/api-probe/api-probe.txt`` (uploaded as an
artifact) and a digest is printed as GitHub Actions annotations so the result
can be read without downloading anything.

This file is temporary: it is removed again by the "Remove temporary CI probes"
commit.
"""

from __future__ import annotations

import io
import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "build" / "api-probe"
OUT_FILE = OUT_DIR / "api-probe.txt"
NESTED_DIR = OUT_DIR / "nested"

# Classes whose 26.2 shape drives the port.
JAVAP_TARGETS = [
    "net.minecraft.client.Minecraft",
    "net.minecraft.client.gui.Gui",
    "net.minecraft.client.KeyMapping",
    "net.minecraft.client.gui.screens.Screen",
    "net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper",
    "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper",
    "net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements",
    "net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry",
    "net.fabricmc.fabric.api.resource.v1.ResourceLoader",
]

IMPORT_RE = re.compile(r"^import\s+(?!static\b)([A-Za-z0-9_.$]+)\s*;")


def log(message: str = "") -> None:
    print(message)


def notice(title: str, message: str) -> None:
    """Emit a GitHub Actions annotation (max ~10 per step, so keep them few)."""
    flat = message.replace("\r", "").replace("\n", "%0A")
    print(f"::notice title={title}::{flat}")


def gather_jars() -> list[str]:
    jars: list[str] = []
    seen: set[str] = set()

    classpath_file = OUT_DIR / "classpath.txt"
    if classpath_file.exists():
        for raw in classpath_file.read_text(encoding="utf-8", errors="replace").splitlines():
            candidate = raw.strip()
            if candidate.startswith("#") or not candidate.endswith(".jar"):
                continue
            if os.path.isfile(candidate) and candidate not in seen:
                seen.add(candidate)
                jars.append(candidate)

    if not jars:
        cache = Path(os.path.expanduser("~/.gradle/caches"))
        if cache.is_dir():
            for path in sorted(cache.rglob("*.jar")):
                name = path.name.lower()
                if any(token in name for token in ("minecraft", "fabric-api", "fabric-loader", "modmenu")):
                    text = str(path)
                    if text not in seen:
                        seen.add(text)
                        jars.append(text)

    return jars


def index_classes(jars: list[str]) -> tuple[dict[str, str], list[str]]:
    """Map FQN -> origin jar, unpacking jar-in-jar entries on the way."""
    index: dict[str, str] = {}
    nested_jars: list[str] = []
    problems: list[str] = []

    for jar in jars:
        try:
            with zipfile.ZipFile(jar) as archive:
                for info in archive.infolist():
                    if info.filename.endswith(".class"):
                        fqn = info.filename[: -len(".class")].replace("/", ".")
                        index.setdefault(fqn, jar)
                    elif info.filename.endswith(".jar"):
                        try:
                            data = archive.read(info)
                            NESTED_DIR.mkdir(parents=True, exist_ok=True)
                            target = NESTED_DIR / Path(info.filename).name
                            if not target.exists():
                                target.write_bytes(data)
                            nested_jars.append(str(target))
                            with zipfile.ZipFile(io.BytesIO(data)) as nested:
                                for nested_info in nested.infolist():
                                    if nested_info.filename.endswith(".class"):
                                        fqn = nested_info.filename[: -len(".class")].replace("/", ".")
                                        index.setdefault(fqn, f"{jar}!{info.filename}")
                        except Exception as exc:  # noqa: BLE001 - probe must never fail
                            problems.append(f"nested jar {info.filename} in {jar}: {exc}")
        except Exception as exc:  # noqa: BLE001 - probe must never fail
            problems.append(f"cannot read {jar}: {exc}")

    return index, nested_jars


def collect_imports() -> list[str]:
    imports: set[str] = set()
    for source_root in ("src/main/java", "src/client/java", "src/test/java"):
        base = ROOT / source_root
        if not base.is_dir():
            continue
        for path in sorted(base.rglob("*.java")):
            try:
                text = path.read_text(encoding="utf-8", errors="replace")
            except Exception:  # noqa: BLE001
                continue
            for line in text.splitlines():
                match = IMPORT_RE.match(line.strip())
                if match:
                    imports.add(match.group(1))
    return sorted(imports)


def javap(classpath: str, fqn: str) -> tuple[str, str]:
    """Return (status, output) for a javap run against the given classpath."""
    try:
        proc = subprocess.run(
            ["javap", "-p", "-classpath", classpath, fqn],
            capture_output=True,
            text=True,
            timeout=180,
        )
    except Exception as exc:  # noqa: BLE001
        return "ERROR", str(exc)

    output = (proc.stdout or "") + (proc.stderr or "")
    if proc.returncode != 0:
        return "ERROR", output.strip() or f"javap exited with {proc.returncode}"
    return "OK", output


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    report: list[str] = []

    report.append("=== Minecraft 26.2 API probe ===")
    report.append(f"repository: {ROOT}")
    report.append("")

    jars = gather_jars()
    report.append("=== resolved jars ===")
    for jar in jars:
        report.append(jar)
    if not jars:
        report.append("(no jars found: the build never got far enough to download them)")
    report.append("")

    index, nested_jars = index_classes(jars)
    report.append("=== jar-in-jar entries extracted for javap ===")
    for nested in nested_jars:
        report.append(nested)
    report.append("")

    imports = collect_imports()
    report.append("=== imports used by the mod sources ===")
    for name in imports:
        report.append(name)
    report.append("")

    report.append("=== imported FQN presence in the resolved jars ===")
    missing: list[str] = []
    present = 0
    for name in imports:
        origin = index.get(name)
        if origin is None:
            missing.append(name)
            report.append(f"MISSING  {name}")
        else:
            present += 1
            report.append(f"present  {name}  <- {origin}")
    report.append("")
    report.append(f"present: {present}   missing: {len(missing)}")
    report.append("")

    classpath = os.pathsep.join(jars + nested_jars)
    report.append("=== javap ===")
    javap_digest: list[str] = []
    for fqn in JAVAP_TARGETS:
        status, output = javap(classpath, fqn)
        report.append("")
        report.append(f"--- {fqn} ({status}) ---")
        report.append(output if output else "(no output)")
        if status != "OK":
            javap_digest.append(f"{fqn}: {status}")
            continue
        interesting = [
            line.strip()
            for line in output.splitlines()
            if re.search(r"setScreen|screen|getInstance|\bGui\b|KeyMapping\(|registerKey|STATUS_EFFECTS|MOB_EFFECTS|registerReload", line)
        ]
        for line in interesting[:12]:
            javap_digest.append(f"{fqn}: {line}")

    report.append("")
    report.append("=== javap digest ===")
    for line in javap_digest:
        report.append(line)

    OUT_FILE.write_text("\n".join(report) + "\n", encoding="utf-8")
    log(f"api-probe: report written to {OUT_FILE}")

    notice(
        "API probe jars",
        "\n".join(jars) if jars else "(no jars resolved)",
    )
    notice(
        "API probe missing imports",
        "\n".join(missing) if missing else "(every imported FQN was found in the resolved jars)",
    )
    notice(
        "API probe javap",
        "\n".join(javap_digest) if javap_digest else "(no javap output)",
    )
    notice(
        "API probe summary",
        f"imports={len(imports)} present={present} missing={len(missing)} jars={len(jars)}",
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
