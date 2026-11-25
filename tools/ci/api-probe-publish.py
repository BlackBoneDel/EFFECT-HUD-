#!/usr/bin/env python3
"""Temporary CI helper: publish the API probe output through the Checks API.

GitHub truncates workflow annotations (50 per check run, 4096 characters per
message), so the full ``javap`` evidence does not fit there. This helper posts
the probe output as additional check runs whose ``output.text`` field holds the
complete dump, which the REST API returns verbatim::

    GET /repos/{owner}/{repo}/check-runs/{check_run_id}

The same text is also written to the job step summary and kept in the uploaded
artifact, so the evidence can be read from the run page, the summary and the
API.

This file is temporary: it is removed again by the "Remove temporary CI probes"
commit.
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "build" / "api-probe"
BLOCKS_DIR = OUT_DIR / "blocks"
REPORT = OUT_DIR / "report.txt"
SUMMARY = OUT_DIR / "javap-full.txt"

# The Checks API caps output.text at 65535 characters.
MAX_CHUNK = 60000


def collect() -> str:
    sections: list[str] = []
    if REPORT.exists():
        sections.append(REPORT.read_text(encoding="utf-8", errors="replace").rstrip())
    if BLOCKS_DIR.is_dir():
        for path in sorted(BLOCKS_DIR.glob("*.txt")):
            body = path.read_text(encoding="utf-8", errors="replace").rstrip()
            sections.append(f"=== {path.stem} ===\n{body}")
    return "\n\n".join(sections) + "\n"


def split(text: str) -> list[str]:
    chunks: list[str] = []
    current: list[str] = []
    size = 0
    for line in text.splitlines():
        if current and size + len(line) + 1 > MAX_CHUNK:
            chunks.append("\n".join(current))
            current = []
            size = 0
        current.append(line)
        size += len(line) + 1
    if current:
        chunks.append("\n".join(current))
    return chunks


def write_summary(text: str) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as handle:
            handle.write("## Minecraft 26.2 API probe (javap)\n\n```text\n")
            handle.write(text)
            handle.write("\n```\n")
    if SUMMARY.exists():
        SUMMARY.write_text(text, encoding="utf-8")


def publish(chunks: list[str]) -> int:
    token = os.environ.get("GITHUB_TOKEN", "")
    api_url = os.environ.get("GITHUB_API_URL", "https://api.github.com")
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    head_sha = os.environ.get("PROBE_HEAD_SHA") or os.environ.get("GITHUB_SHA", "")
    if not token or not repository or not head_sha:
        print("api-probe-publish: missing GITHUB_TOKEN/GITHUB_REPOSITORY/PROBE_HEAD_SHA")
        return 0

    total = len(chunks)
    for index, chunk in enumerate(chunks, start=1):
        name = f"API probe javap ({index}/{total})"
        body = {
            "name": name,
            "head_sha": head_sha,
            "status": "completed",
            "conclusion": "success",
            "details_url": os.environ.get("PROBE_DETAILS_URL", "") or None,
            "output": {
                "title": name,
                "summary": f"Minecraft 26.2 API probe, javap part {index} of {total}.",
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
                "User-Agent": "effect-hud-api-probe",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                payload = json.loads(response.read().decode("utf-8"))
            print(f"api-probe-publish: created check run {payload.get('id')} ({name})")
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")[:500]
            print(f"api-probe-publish: HTTP {error.code} for {name}: {detail}")
        except Exception as error:  # noqa: BLE001 - the probe must never fail
            print(f"api-probe-publish: failed for {name}: {error}")
    return 0


def main(argv: list[str]) -> int:
    text = collect()
    if not text.strip():
        print("api-probe-publish: nothing to publish")
        return 0
    SUMMARY.parent.mkdir(parents=True, exist_ok=True)
    SUMMARY.write_text(text, encoding="utf-8")
    write_summary(text)
    chunks = split(text)
    print(f"api-probe-publish: {len(text)} characters in {len(chunks)} chunk(s)")
    if "--dry-run" in argv:
        for index, chunk in enumerate(chunks, start=1):
            print(f"--- chunk {index}/{len(chunks)}: {len(chunk)} characters ---")
        return 0
    return publish(chunks)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
