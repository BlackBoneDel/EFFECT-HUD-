#!/usr/bin/env python3
"""Publish the PIT log of the mutation job through the Checks API.

The workflow log of a job cannot be downloaded reliably (the log archive
endpoint answers with an EOF on some runners), and annotations are capped at
4096 characters per message. The complete ``pit.log`` is therefore posted as
extra check runs whose ``output.text`` field holds the verbatim dump, which the
REST API returns without truncation::

    GET /repos/{owner}/{repo}/check-runs/{check_run_id}

The same text is written to the job step summary.
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

# The Checks API caps output.text at 65535 characters.
MAX_CHUNK = 60000


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


def head_and_tail(text: str) -> list[str]:
    """Keep the beginning and the end of a long log.

    A verbose PIT run produces megabytes of output, but the interesting parts
    are the startup of the minion (head) and the failure (tail).
    """
    lines = text.splitlines()
    if len(text) <= MAX_CHUNK * 2:
        return split(text)
    head = lines[:400]
    tail = lines[-400:]
    skipped = len(lines) - len(head) - len(tail)
    return [
        "\n".join(head),
        f"... {skipped} lines skipped ...",
        "\n".join(tail),
    ]


def write_summary(text: str) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as handle:
            handle.write("## PIT log\n\n```text\n")
            handle.write(text)
            handle.write("\n```\n")


def publish(chunks: list[str]) -> int:
    token = os.environ.get("GITHUB_TOKEN", "")
    api_url = os.environ.get("GITHUB_API_URL", "https://api.github.com")
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    head_sha = os.environ.get("PIT_HEAD_SHA") or os.environ.get("GITHUB_SHA", "")
    if not token or not repository or not head_sha:
        print("pit-log-publish: missing GITHUB_TOKEN/GITHUB_REPOSITORY/PIT_HEAD_SHA")
        return 0

    total = len(chunks)
    for index, chunk in enumerate(chunks, start=1):
        name = f"PIT log ({index}/{total})"
        body = {
            "name": name,
            "head_sha": head_sha,
            "status": "completed",
            "conclusion": "success",
            "details_url": os.environ.get("PIT_DETAILS_URL", "") or None,
            "output": {
                "title": name,
                "summary": f"PIT log, part {index} of {total}.",
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
                "User-Agent": "effect-hud-pit",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                payload = json.loads(response.read().decode("utf-8"))
            print(f"pit-log-publish: created check run {payload.get('id')} ({name})")
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")[:500]
            print(f"pit-log-publish: HTTP {error.code} for {name}: {detail}")
        except Exception as error:  # noqa: BLE001 - the report must never fail the job
            print(f"pit-log-publish: failed for {name}: {error}")
    return 0


def main(argv: list[str]) -> int:
    path = Path(argv[1]) if len(argv) > 1 else Path("pit.log")
    if not path.exists():
        print(f"pit-log-publish: {path} does not exist")
        return 0
    text = path.read_text(encoding="utf-8", errors="replace")
    chunks = head_and_tail(text)
    write_summary("\n\n".join(chunks))
    print(f"pit-log-publish: {len(text)} characters in {len(chunks)} chunk(s)")
    return publish(chunks)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
