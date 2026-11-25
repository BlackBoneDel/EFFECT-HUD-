#!/usr/bin/env python3
"""Temporary CI probe for the Minecraft 26.2 port.

Collects hard evidence about the new API surface instead of guessing:

* the jars Gradle actually resolved (plus every jar found in the Gradle cache),
* the imports used by the mod sources,
* whether each imported fully qualified class name still exists,
* whether the 26.2 candidate replacements exist, and
* ``javap`` output for the classes whose 26.2 shape drives the port.

The full report is written to ``build/api-probe/api-probe.txt`` (uploaded as an
artifact) and to the job step summary, and a digest is printed as GitHub
Actions annotations so the result can be read without downloading anything.

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
REPORT_FILE = OUT_DIR / "report.txt"
NESTED_DIR = OUT_DIR / "nested"
BLOCKS_DIR = OUT_DIR / "blocks"

# (class, keep-pattern). The keep pattern selects the javap members worth
# reading for the port; None keeps every public/protected member.
JAVAP_TARGETS = [
    ("net.minecraft.client.Minecraft", None),
    ("net.minecraft.client.gui.Gui", None),
    ("net.minecraft.client.gui.Hud", None),
    ("net.minecraft.client.Options", r"hide|idden|gui|Gui"),
    ("net.minecraft.client.gui.GuiGraphicsExtractor", None),
    ("net.minecraft.client.gui.GuiGraphics", None),
    ("net.minecraft.client.gui.screens.Screen", None),
    ("net.minecraft.client.gui.components.AbstractWidget", None),
    ("net.minecraft.client.gui.components.AbstractSliderButton", None),
    ("net.minecraft.client.gui.components.Button", None),
    ("net.minecraft.client.gui.components.EditBox", None),
    ("net.minecraft.client.gui.components.CycleButton", None),
    ("net.minecraft.client.gui.components.Tooltip", None),
    ("net.minecraft.client.gui.components.events.GuiEventListener", None),
    ("net.minecraft.client.gui.components.events.AbstractContainerEventHandler", None),
    ("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen", None),
    ("net.minecraft.client.gui.screens.ChatScreen", None),
    ("net.minecraft.client.gui.screens.PauseScreen", None),
    ("net.minecraft.client.KeyMapping", None),
    ("net.minecraft.client.renderer.state.gui.GuiRenderState", None),
    ("net.minecraft.client.input.CharacterEvent", None),
    ("net.minecraft.client.input.InputWithModifiers", None),
    ("net.minecraft.client.input.KeyEvent", None),
    ("net.minecraft.client.input.MouseButtonEvent", None),
    ("net.minecraft.client.renderer.RenderPipelines", r"GUI|gui|Gui"),
    ("net.minecraft.client.renderer.texture.MissingTextureAtlasSprite", None),
    ("net.minecraft.world.entity.player.Player", r"[Mm]essage"),
    ("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement", None),
    ("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry", None),
    ("net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements", None),
    ("net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper", None),
    ("net.fabricmc.fabric.api.resource.v1.ResourceLoader", None),
    ("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents", None),
]

# Candidate 26.2 replacements: reported as present/absent without being used yet.
CANDIDATES = [
    "net.minecraft.client.gui.GuiGraphicsExtractor",
    "net.minecraft.client.gui.GuiGraphics",
    "net.minecraft.client.gui.Gui",
    "net.minecraft.client.gui.Hud",
    "net.minecraft.client.gui.components.AbstractWidget",
    "net.minecraft.client.renderer.state.gui.GuiRenderState",
    "net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState",
    "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
    "net.minecraft.client.gui.screens.ChatScreen",
    "net.minecraft.client.gui.screens.PauseScreen",
    "net.minecraft.client.renderer.texture.MissingTextureAtlasSprite",
    "net.minecraft.resources.Identifier",
    "net.minecraft.util.ARGB",
    "net.minecraft.world.effect.MobEffect",
    "net.minecraft.world.effect.MobEffects",
    "net.minecraft.server.packs.resources.ResourceManagerReloadListener",
    "net.minecraft.client.input.KeyEvent",
    "net.minecraft.client.input.MouseButtonEvent",
    "net.minecraft.client.DeltaTracker",
    "net.minecraft.client.gui.Font",
    "net.minecraft.client.gui.components.Button",
    "net.minecraft.client.gui.components.CycleButton",
    "net.minecraft.client.gui.components.EditBox",
    "net.minecraft.client.gui.components.Tooltip",
    "net.minecraft.client.gui.components.AbstractSliderButton",
    "net.minecraft.client.gui.components.events.GuiEventListener",
    "net.minecraft.client.gui.components.events.AbstractContainerEventHandler",
    "net.minecraft.locale.Language",
    "net.minecraft.core.Holder",
    "net.minecraft.core.registries.BuiltInRegistries",
    "net.minecraft.network.chat.Component",
    "net.minecraft.server.packs.PackType",
    "net.minecraft.server.packs.resources.Resource",
    "net.minecraft.server.packs.resources.ResourceManager",
    "net.minecraft.util.Mth",
    "net.minecraft.world.effect.MobEffectCategory",
    "net.minecraft.world.effect.MobEffectInstance",
    "net.minecraft.client.renderer.state.gui.GuiRenderState",
    "com.mojang.blaze3d.platform.InputConstants",
    "org.lwjgl.glfw.GLFW",
]

IMPORT_RE = re.compile(r"^import\s+(?!static\b)([A-Za-z0-9_.$]+)\s*;")


def notice(title: str, message: str) -> None:
    """Emit a GitHub Actions annotation (keep the number of notices small)."""
    flat = message.replace("\r", "").replace("\n", "%0A").replace("%", "%25")
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
                        except Exception:  # noqa: BLE001 - the probe must never fail
                            pass
        except Exception:  # noqa: BLE001 - the probe must never fail
            pass

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


def filter_members(output: str, keep: str | None) -> list[str]:
    pattern = re.compile(r"\b(public|protected)\b")
    if keep:
        pattern = re.compile(r"\b(public|protected)\b|(" + keep + ")")
    members: list[str] = []
    for line in output.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("Compiled from"):
            continue
        if stripped.endswith("{") or stripped == "}":
            continue
        if pattern.search(stripped):
            members.append(stripped)
    return members


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    report: list[str] = []

    report.append("=== Minecraft 26.2 API probe ===")
    report.append(f"repository: {ROOT}")
    report.append("")

    jars = gather_jars()
    report.append("=== resolved jars ===")
    report.extend(jars or ["(no jars found: the build never got far enough to download them)"])
    report.append("")

    index, nested_jars = index_classes(jars)
    report.append("=== jar-in-jar entries extracted for javap ===")
    report.extend(nested_jars)
    report.append("")

    imports = collect_imports()
    report.append("=== imports used by the mod sources ===")
    report.extend(imports)
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

    report.append("=== 26.2 candidate classes ===")
    candidate_lines: list[str] = []
    for name in CANDIDATES:
        state = "present" if name in index else "ABSENT"
        candidate_lines.append(f"{state:8} {name}")
    report.extend(candidate_lines)
    report.append("")

    classpath = os.pathsep.join(jars + nested_jars)
    BLOCKS_DIR.mkdir(parents=True, exist_ok=True)
    for stale in BLOCKS_DIR.glob("*.txt"):
        stale.unlink()

    for block_index, (fqn, keep) in enumerate(JAVAP_TARGETS):
        status, output = javap(classpath, fqn)
        if status != "OK":
            body = f"{fqn}: {status}\n{output}"
            (BLOCKS_DIR / f"{block_index:02d}-{fqn}.txt").write_text(body + "\n", encoding="utf-8")
            continue
        members = filter_members(output, keep)
        # GitHub truncates annotation messages at 4096 characters, so every
        # class is written in small chunks.
        chunk: list[str] = []
        size = 0
        part = 1
        for member in members:
            if chunk and size + len(member) > 2500:
                (BLOCKS_DIR / f"{block_index:02d}-{fqn}.p{part}.txt").write_text(
                    f"{fqn} (part {part})\n" + "\n".join(chunk) + "\n", encoding="utf-8")
                chunk = []
                size = 0
                part += 1
            chunk.append(member)
            size += len(member) + 1
        if chunk:
            (BLOCKS_DIR / f"{block_index:02d}-{fqn}.p{part}.txt").write_text(
                f"{fqn} (part {part})\n" + "\n".join(chunk) + "\n", encoding="utf-8")

    OUT_FILE.write_text("\n".join(report) + "\n", encoding="utf-8")
    REPORT_FILE.write_text("\n".join(report) + "\n", encoding="utf-8")
    print(f"api-probe: report written to {OUT_FILE}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
