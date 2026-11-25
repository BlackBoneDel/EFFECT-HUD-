#!/usr/bin/env python3
"""Temporary CI helper: emit the API probe report as GitHub Actions annotations.

GitHub accepts at most 10 annotations of each type per step, so the report is
split into small blocks and emitted a few per invocation.

Usage:
    python3 tools/ci/api-probe-emit.py <start> <count>   # javap blocks
    python3 tools/ci/api-probe-emit.py --summary         # jars/imports/candidates
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "build" / "api-probe"
BLOCKS_DIR = OUT_DIR / "blocks"
REPORT = OUT_DIR / "report.txt"


def notice(title: str, message: str) -> None:
    flat = message.replace("\r", "").replace("\n", "%0A").replace("%", "%25")
    print(f"::notice title={title}::{flat}")


def emit_summary() -> int:
    if not REPORT.exists():
        print("api-probe-emit: no report yet")
        return 0
    text = REPORT.read_text(encoding="utf-8", errors="replace")
    sections = text.split("\n=== ")
    for section in sections[1:]:
        name = section.split("\n", 1)[0].strip()
        body = section[len(name):].strip()
        notice(f"API probe {name}", body if body else "(empty)")
    return 0


def emit_blocks(start: int, count: int) -> int:
    if not BLOCKS_DIR.is_dir():
        print("api-probe-emit: no blocks yet")
        return 0
    blocks = sorted(BLOCKS_DIR.glob("*.txt"))
    for path in blocks[start:start + count]:
        notice(f"API probe {path.stem}", path.read_text(encoding="utf-8", errors="replace").strip())
    return 0


def main(argv: list[str]) -> int:
    if len(argv) >= 2 and argv[1] == "--summary":
        return emit_summary()
    start = int(argv[1]) if len(argv) > 1 else 0
    count = int(argv[2]) if len(argv) > 2 else 10
    return emit_blocks(start, count)


if __name__ == "__main__":
    sys.exit(main(sys.argv))
