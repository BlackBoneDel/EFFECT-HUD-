#!/usr/bin/env python3
"""CI helper: turn the Gradle log into readable diagnostics.

javac prints the errors in several shapes depending on the failure, so this
helper collects every line that mentions an error, keeps the "symbol:" and
"location:" follow-ups that belong to it, and writes the result to the job step
summary plus GitHub Actions annotations.

The raw log is published through the Checks API, because the workflow log of a
job cannot be downloaded reliably (the log archive endpoint answers with an
EOF on some runners).
"""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ERROR_RE = re.compile(r"^(.*\.java):(\d+):\s*error:\s*(.*)$")
FOLLOW_UP_RE = re.compile(r"^\s+(symbol|location):")
ANY_ERROR_RE = re.compile(r"error:", re.IGNORECASE)
SUMMARY_HEADER = "## Compiler diagnostics"


def collect(text: str) -> tuple[list[list[str]], list[str]]:
    """Splits the log into real compiler errors and other error-looking lines.

    javac reports errors as ``path:line: error: message``; anything else that
    merely mentions "error" (a Gradle message, a stack trace, ...) is kept
    apart, so a green build does not report phantom compiler errors.
    """
    errors: list[list[str]] = []
    others: list[str] = []
    current: list[str] | None = None
    for raw in text.splitlines():
        line = raw.rstrip()
        stripped = line.strip()
        match = ERROR_RE.match(stripped)
        if match:
            if current:
                errors.append(current)
            current = [f"{match.group(1)}:{match.group(2)}: error: {match.group(3)}"]
            continue
        if current is not None and FOLLOW_UP_RE.match(line):
            current.append(stripped)
            continue
        if current is not None and stripped and not line.startswith((" ", "\t")):
            errors.append(current)
            current = None
        if current is None and ANY_ERROR_RE.search(stripped) and len(stripped) < 400:
            others.append(stripped)
    if current:
        errors.append(current)
    return errors, others


def write_summary(errors: list[list[str]]) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write(f"{SUMMARY_HEADER}\n\n```\n")
        for entry in errors:
            handle.write(" | ".join(entry) + "\n")
        handle.write("```\n")


def annotate(errors: list[list[str]], compiler_errors: int) -> None:
    for index in range(0, min(len(errors), 100), 10):
        chunk = errors[index:index + 10]
        body = "%0A".join(" | ".join(entry).replace("%", "%25") for entry in chunk)
        print(f"::error title=Compile error::{body}")
    if compiler_errors:
        print(f"::notice title=Compile errors::{compiler_errors} compiler error(s) detected")
    else:
        print("::notice title=Compile errors::no compiler error in the log")


def publish(text: str) -> None:
    """Publish the raw Gradle log through the Checks API so it can be read back."""
    token = os.environ.get("GITHUB_TOKEN", "")
    api_url = os.environ.get("GITHUB_API_URL", "https://api.github.com")
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    head_sha = os.environ.get("CI_HEAD_SHA") or os.environ.get("GITHUB_SHA", "")
    if not token or not repository or not head_sha or not text.strip():
        return
    chunks: list[str] = []
    current: list[str] = []
    size = 0
    for line in text.splitlines():
        if current and size + len(line) + 1 > 60000:
            chunks.append("\n".join(current))
            current = []
            size = 0
        current.append(line)
        size += len(line) + 1
    if current:
        chunks.append("\n".join(current))
    for index, chunk in enumerate(chunks, start=1):
        name = f"Gradle build log ({index}/{len(chunks)})"
        body = {
            "name": name,
            "head_sha": head_sha,
            "status": "completed",
            "conclusion": "success",
            "details_url": os.environ.get("CI_DETAILS_URL", "") or None,
            "output": {
                "title": name,
                "summary": f"Gradle build log, part {index} of {len(chunks)}.",
                "text": chunk,
            },
        }
        request = urllib.request.Request(
            f"{api_url}/repos/{repository}/check-runs",
            data=json.dumps(body).encode("utf-8"),
            headers={
                "Accept": "application/vnd.github+json",
                "Authorization": f"Bearer {token}",
                "X-GitHub-Api-Version": "2022-11-28",
                "Content-Type": "application/json",
                "User-Agent": "effect-hud-compile-diagnostics",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                payload = json.loads(response.read().decode("utf-8"))
            print(f"compile-diagnostics: published check run {payload.get('id')} ({name})")
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")[:300]
            print(f"compile-diagnostics: HTTP {error.code}: {detail}")
        except Exception as error:  # noqa: BLE001
            print(f"compile-diagnostics: failed to publish: {error}")


def main(argv: list[str]) -> int:
    path = Path(argv[1]) if len(argv) > 1 else Path("/tmp/gradle.log")
    text = path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""
    errors, others = collect(text)
    publish(text)
    compiler_errors = len(errors)
    if not errors and not others:
        # Nothing looks like an error: show the tail of the log anyway, so a
        # failure is never silent.
        tail = text[-3000:] if text else "(no gradle log)"
        errors = [[line] for line in tail.splitlines() if line.strip()][-20:]
    else:
        errors = errors + [[line] for line in others[:20]]
    write_summary(errors)
    annotate(errors, compiler_errors)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
