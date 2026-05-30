"""Concurrency test helpers — TBD when concurrency suite is built (Task 10/L3)."""
from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any, Callable


def run_concurrent(fn: Callable[[], Any], *, n: int, max_workers: int | None = None) -> list[Any]:
    """Run `fn` concurrently `n` times. Returns list of results (exceptions included)."""
    workers = max_workers or min(n, 10)
    results: list[Any] = []
    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = [pool.submit(fn) for _ in range(n)]
        for future in as_completed(futures):
            try:
                results.append(future.result())
            except Exception as exc:
                results.append(exc)
    return results


def partition(results: list[Any]) -> tuple[list[Any], list[Exception]]:
    """Split results into (successes, failures)."""
    successes = [r for r in results if not isinstance(r, Exception)]
    failures = [r for r in results if isinstance(r, Exception)]
    return successes, failures
