"""HTTP response helpers — assert_ok, assert_blocked, get_data."""
from __future__ import annotations

from typing import Any, Iterable

import httpx


def assert_ok(response: httpx.Response, *, expected: Iterable[int] = (200, 201, 202, 204)) -> None:
    if response.status_code not in expected:
        raise AssertionError(
            f"Expected {tuple(expected)} from {response.request.method} {response.request.url}, "
            f"got {response.status_code}: {response.text}"
        )


def assert_blocked(
    response: httpx.Response,
    *,
    allow: Iterable[int] = (400, 403, 409, 422, 423),
) -> dict[str, Any]:
    """Assert the response is a known blocker status. Returns the parsed body."""
    if response.status_code not in allow:
        raise AssertionError(
            f"Expected one of {tuple(allow)} (blocker) from "
            f"{response.request.method} {response.request.url}, "
            f"got {response.status_code}: {response.text}"
        )
    try:
        return response.json()
    except Exception:
        return {}


def get_data(response: httpx.Response) -> dict[str, Any]:
    """Return response.json()['data'], raising AssertionError on bad shape."""
    assert_ok(response)
    body = response.json()
    if not isinstance(body, dict) or "data" not in body:
        raise AssertionError(
            f"Response from {response.request.url} has no 'data' key: {response.text}"
        )
    return body["data"]
