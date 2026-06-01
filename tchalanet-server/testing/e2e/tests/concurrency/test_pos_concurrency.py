"""POS concurrency correctness (L3).

Small, controlled parallelism (2–10 requests) that asserts *correctness*, not
throughput/latency (perf is a separate future suite — see design §2).

Covered here (single tenant, safe against the shared session fixture):
  - concurrent same idempotency key + same payload  → exactly ONE ticket, no 5xx
  - concurrent same idempotency key + diff payloads  → ≤1 accepted, others clean 4xx
  - concurrent DISTINCT keys (valid sells)           → all accepted, distinct tickets

Deferred (need Tenant B / explicit limit config — wired with TCH_TENANT_2_*):
  - limit exposure race (not all overexposing sells succeed)
  - parallel Tenant A/B sales do not leak data/config
  - session close vs sell race
"""
from __future__ import annotations

import uuid
from concurrent.futures import ThreadPoolExecutor

import pytest

from fixtures.pos_context import PosContext
from prereqs.draws import ensure_draws_today
from tch_e2e.concurrency import partition, run_concurrent


_N = 5  # controlled parallelism


def _require_draw_and_game(ctx: PosContext) -> tuple[dict, str]:
    draw = ctx.first_open_draw()
    if draw is None:
        pytest.skip("No OPEN draw available.")
    game_codes = draw.get("gameCodes") or []
    if not game_codes:
        pytest.skip(f"Draw {draw.get('drawId')} exposes no gameCodes.")
    return draw, game_codes[0]


def _statuses(results: list) -> list[int]:
    ok, _ = partition(results)
    return [r.status_code for r in ok]


def _no_5xx(results: list) -> None:
    succ, exc = partition(results)
    assert not exc, f"Concurrent requests raised exceptions: {exc!r}"
    server_errors = [r for r in succ if r.status_code >= 500]
    assert not server_errors, (
        "Concurrent sells must never 500. Got: "
        + ", ".join(f"{r.status_code} {r.text[:160]}" for r in server_errors)
    )


def _ticket_ids_of_2xx(results: list) -> set[str]:
    ids: set[str] = set()
    for r in results:
        if 200 <= r.status_code < 300:
            tid = (r.json().get("data") or {}).get("ticketId")
            if tid:
                ids.add(tid)
    return ids


# ===========================================================================
# Idempotency under real concurrency
# ===========================================================================


@pytest.mark.L3
@pytest.mark.cashier_pos
def test_concurrent_same_key_same_payload_creates_one_ticket(
    onboard_cashier_for_pos: PosContext,
    super_admin_client,
    seed_ids,
) -> None:
    """N concurrent sells with the same key + same payload create exactly one ticket.

    Winner → STARTED (ACCEPTED). Concurrent peers → either 200 replay (same ticketId)
    or 409 ``idempotency.in_progress``. Never a 500 (the begin() check-first +
    aborted-tx guard is what this exercises) and never a second ticket.
    """
    ctx = onboard_cashier_for_pos
    ensure_draws_today(super_admin_client, seed_ids)
    draw, game_code = _require_draw_and_game(ctx)

    payload = ctx.cashier_flow()._sale_payload(draw, [game_code])
    idem_key = str(uuid.uuid4())
    op_ctx = ctx.op_context()

    def fire():
        return ctx.cashier_client.post(
            "/tenant/cashier/tickets/sell",
            json=payload,
            context=op_ctx,
            idempotency_key=idem_key,
        )

    results = run_concurrent(fire, n=_N)
    _no_5xx(results)

    succ, _ = partition(results)
    accepted_codes = {200, 201}
    conflict_codes = {409}
    for r in succ:
        assert r.status_code in accepted_codes | conflict_codes, (
            f"Unexpected status under idempotency race: {r.status_code} {r.text[:200]}"
        )

    ticket_ids = _ticket_ids_of_2xx(succ)
    assert len(ticket_ids) == 1, (
        f"Idempotency race must yield exactly one ticket, got ticketIds={ticket_ids} "
        f"from statuses={_statuses(results)}"
    )


@pytest.mark.L3
@pytest.mark.cashier_pos
def test_concurrent_same_key_different_payload_rejected_cleanly(
    onboard_cashier_for_pos: PosContext,
    super_admin_client,
    seed_ids,
) -> None:
    """N concurrent sells, same key but different payloads → at most one accepted.

    Peers must get a clean 4xx (409 payload_mismatch / in_progress), never a 500.
    """
    ctx = onboard_cashier_for_pos
    ensure_draws_today(super_admin_client, seed_ids)
    draw, game_code = _require_draw_and_game(ctx)

    base = ctx.cashier_flow()._sale_payload(draw, [game_code])
    idem_key = str(uuid.uuid4())
    op_ctx = ctx.op_context()

    # Each thread sends a distinct selection under the *same* idempotency key.
    selections = ["11", "22", "33", "44", "55"][:_N]

    def fire(selection: str):
        payload = {**base, "lines": [{**base["lines"][0], "selection": selection}]}
        return ctx.cashier_client.post(
            "/tenant/cashier/tickets/sell",
            json=payload,
            context=op_ctx,
            idempotency_key=idem_key,
        )

    with ThreadPoolExecutor(max_workers=len(selections)) as pool:
        results = list(pool.map(fire, selections))
    _no_5xx(results)

    succ, _ = partition(results)
    accepted = [r for r in succ if 200 <= r.status_code < 300]
    rejected = [r for r in succ if r.status_code in (400, 409, 422)]
    assert len(accepted) <= 1, (
        f"At most one payload may win the key, got {len(accepted)} accepted: "
        f"{_statuses(results)}"
    )
    assert len(accepted) + len(rejected) == len(succ), (
        f"Every response must be 2xx winner or clean 4xx, got {_statuses(results)}"
    )
    assert len(_ticket_ids_of_2xx(succ)) <= 1


@pytest.mark.L3
@pytest.mark.cashier_pos
def test_concurrent_distinct_keys_all_create_tickets(
    onboard_cashier_for_pos: PosContext,
    super_admin_client,
    seed_ids,
) -> None:
    """N concurrent sells with DISTINCT keys + valid payloads → N distinct tickets.

    Baseline that parallel sells on one tenant/session don't interfere or corrupt
    (no lost updates, no shared-state bleed, no 5xx)."""
    ctx = onboard_cashier_for_pos
    ensure_draws_today(super_admin_client, seed_ids)
    draw, game_code = _require_draw_and_game(ctx)

    payload = ctx.cashier_flow()._sale_payload(draw, [game_code])
    op_ctx = ctx.op_context()

    def fire():
        return ctx.cashier_client.post(
            "/tenant/cashier/tickets/sell",
            json=payload,
            context=op_ctx,
            idempotency_key=str(uuid.uuid4()),  # distinct key per call
        )

    results = run_concurrent(fire, n=_N)
    _no_5xx(results)

    succ, _ = partition(results)
    accepted = [r for r in succ if 200 <= r.status_code < 300]
    # All independent sells should succeed (no idempotency collision).
    assert len(accepted) == _N, (
        f"Expected {_N} independent sells to succeed, got statuses={_statuses(results)}"
    )
    ticket_ids = _ticket_ids_of_2xx(succ)
    assert len(ticket_ids) == _N, (
        f"Distinct-key concurrent sells must create {_N} distinct tickets, "
        f"got {len(ticket_ids)}: {ticket_ids}"
    )


# ===========================================================================
# Deferred — need Tenant B / explicit limit config (TCH_TENANT_2_*)
# ===========================================================================


@pytest.mark.L3
@pytest.mark.skip(reason="needs Tenant B with an explicit exposure limit (TCH_TENANT_2_* + limit config)")
def test_limit_exposure_race_blocks_overexposure() -> None:
    """Controlled race near a configured limit → not all overexposing sells succeed."""


@pytest.mark.L3
@pytest.mark.skip(reason="needs Tenant B (TCH_TENANT_2_*) for parallel cross-tenant sales")
def test_parallel_tenant_a_b_sales_do_not_leak() -> None:
    """Parallel A/B sales: each tenant sees only its own tickets/config (RLS isolation)."""
