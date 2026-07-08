"""Non-prod guard: refuse to load-test a production host by default."""
from __future__ import annotations

import os
from urllib.parse import urlparse

# Hosts we never load-test unless explicitly allowlisted.
_PROD_DENY = ("tchalanet.com",)


def assert_non_prod(base_url: str) -> None:
    host = (urlparse(base_url).hostname or "").lower()
    allow = {
        h.strip().lower()
        for h in os.environ.get("TCH_LOAD_ALLOWED_HOSTS", "").split(",")
        if h.strip()
    }
    if host in allow:
        return
    if any(host == d or host.endswith("." + d) for d in _PROD_DENY):
        raise SystemExit(
            f"Refusing to load-test a production-looking host: {host!r}. "
            f"Set TCH_LOAD_ALLOWED_HOSTS to override for an approved non-prod environment."
        )
