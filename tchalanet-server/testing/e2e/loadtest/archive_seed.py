"""Archive E2E seed runner.

The archive lifecycle is verified through real HTTP endpoints. This helper only prepares old data
that regular business APIs cannot safely backdate across technical/archive tables.
"""
from __future__ import annotations

import shlex
import subprocess
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ArchiveSeedConfig:
    enabled: bool = False
    command: str = ""
    sql_path: Path = Path(__file__).with_name("archive_seed.sql")


def seed_archive_fixture(config: ArchiveSeedConfig) -> None:
    if not config.enabled:
        return
    if not config.command.strip():
        raise RuntimeError(
            "Archive seed requested but no command was provided. Set "
            "TCH_ARCHIVE_SEED_COMMAND, e.g. "
            "'docker exec -i -u postgres tchl-postgres-dev psql -d tchalanet_db'."
        )
    if not config.sql_path.exists():
        raise RuntimeError(f"Archive seed SQL file does not exist: {config.sql_path}")

    try:
        result = subprocess.run(
            shlex.split(config.command),
            input=config.sql_path.read_text(encoding="utf-8"),
            text=True,
            capture_output=True,
            check=False,
            timeout=60,
        )
    except subprocess.TimeoutExpired as exc:
        raise RuntimeError("Archive seed command timed out after 60 seconds") from exc
    if result.returncode != 0:
        raise RuntimeError(
            "Archive seed command failed with exit code "
            f"{result.returncode}.\nSTDOUT:\n{result.stdout[-2000:]}\nSTDERR:\n{result.stderr[-2000:]}"
        )
