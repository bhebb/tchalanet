"""Parameterized layout tests — sells one ticket per scenario and writes the resulting
PDF + ESC_POS to disk so the receipt formatting can be reviewed visually.

Scenarios cover: short ticket, medium ticket, long ticket, every supported betOption per
game type, and edge cases (large potential payout, long selection, mixed currencies).

Output: `tchalanet-app/target/pdf/layouts/{scenario_name}.{pdf,escpos}`

Run with:
    uv run pytest tests/cashier/test_layouts.py -v -s
"""
from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path

import pytest

from flows.cashier import CashierFlow
from lib.api import ApiClient
from lib.context import CashierContext
from lib.ids import SeedIds
from prereqs.app_user import ensure_app_user_synced
from prereqs.draws import ensure_draws_today
from prereqs.session import ensure_pos_session_open


def _artifact_dir() -> Path:
    raw = os.environ.get("TCH_ARTIFACT_DIR")
    if raw:
        base = Path(raw)
    else:
        e2e_root = Path(__file__).resolve().parents[2]
        base = e2e_root.parent.parent / "tchalanet-app" / "target" / "pdf" / "layouts"
    base.mkdir(parents=True, exist_ok=True)
    return base


# ─── Scenarios ────────────────────────────────────────────────────────────

@dataclass
class LineSpec:
    game_code: str
    bet_type: str
    selection: str
    bet_option: int | None = None
    stake: str = "1.00"

    def as_payload_line(self) -> dict:
        return {
            "gameCode": self.game_code,
            "betType": self.bet_type,
            "selection": self.selection,
            "betOption": self.bet_option,
            "stake": self.stake,
        }


@dataclass
class Scenario:
    name: str            # used as PDF filename, must be filesystem-safe
    description: str     # printed in test stdout for context
    lines: list[LineSpec] = field(default_factory=list)


def _short_ticket() -> Scenario:
    return Scenario(
        name="01_short_bolet",
        description="1 ligne BOLET",
        lines=[LineSpec("HT_BOLET", "MATCH_1_2D", "11")],
    )


def _medium_ticket() -> Scenario:
    return Scenario(
        name="02_medium_mixed",
        description="3 jeux (BOLET + MARYAJ + LOTO3), 2 sélections chacun",
        lines=[
            LineSpec("HT_BOLET",  "MATCH_1_2D",    "11"),
            LineSpec("HT_BOLET",  "MATCH_1_2D",    "22"),
            LineSpec("HT_MARYAJ", "MARRIAGE_2D2D", "21-25", 1),
            LineSpec("HT_MARYAJ", "MARRIAGE_2D2D", "33-77", 1),
            LineSpec("HT_LOTO3",  "LOTTO3_3D",     "012",   1),
            LineSpec("HT_LOTO3",  "LOTTO3_3D",     "345",   1),
        ],
    )


def _long_ticket() -> Scenario:
    # ~30 lines on the same channel to stress vertical layout + group repetition.
    bolet = [LineSpec("HT_BOLET", "MATCH_1_2D", f"{n:02d}") for n in range(0, 20)]
    maryaj = [
        LineSpec("HT_MARYAJ", "MARRIAGE_2D2D", f"{a:02d}-{b:02d}", 1)
        for a, b in [(11, 22), (33, 44), (55, 66), (77, 88), (10, 90)]
    ]
    loto = [LineSpec("HT_LOTO5", "LOTTO5_PATTERN", "12345", 1)] * 5
    return Scenario(
        name="03_long_30_lines",
        description="~30 lignes (20 BOLET + 5 MARYAJ + 5 LOTO5)",
        lines=bolet + maryaj + loto,
    )


def _maryaj_options() -> Scenario:
    return Scenario(
        name="10_maryaj_options",
        description="MARYAJ – Ordre exact (1) + Revers / Double (2)",
        lines=[
            LineSpec("HT_MARYAJ", "MARRIAGE_2D2D", "21-25", 1),
            LineSpec("HT_MARYAJ", "MARRIAGE_2D2D", "33-77", 2),
        ],
    )


def _loto3_options() -> Scenario:
    return Scenario(
        name="11_loto3_options",
        description="LOTO 3 – Exact (1) + Box (2)",
        lines=[
            LineSpec("HT_LOTO3", "LOTTO3_3D", "012", 1),
            LineSpec("HT_LOTO3", "LOTTO3_3D", "789", 2),
        ],
    )


def _loto4_options() -> Scenario:
    # Front pair (3) and Back pair (4) take 2-digit selections, not 4.
    return Scenario(
        name="12_loto4_options",
        description="LOTO 4 – Exact (1) + Box (2) + Front pair (3) + Back pair (4)",
        lines=[
            LineSpec("HT_LOTO4", "LOTTO4_PATTERN", "1234", 1),
            LineSpec("HT_LOTO4", "LOTTO4_PATTERN", "5678", 2),
            LineSpec("HT_LOTO4", "LOTTO4_PATTERN", "12",   3),
            LineSpec("HT_LOTO4", "LOTTO4_PATTERN", "78",   4),
        ],
    )


def _loto5_options() -> Scenario:
    return Scenario(
        name="13_loto5_options",
        description="LOTO 5 – Lot1+Lot2 (1), Lot1+Lot3 (2), Lot2+Lot3 (3)",
        lines=[
            LineSpec("HT_LOTO5", "LOTTO5_PATTERN", "12345", 1),
            LineSpec("HT_LOTO5", "LOTTO5_PATTERN", "67890", 2),
            LineSpec("HT_LOTO5", "LOTTO5_PATTERN", "13579", 3),
        ],
    )


def _large_gain_ticket() -> Scenario:
    # Higher stake on LOTO4/5 → blows up potential payout, exercises right-alignment of
    # large amounts in the Gain column.
    return Scenario(
        name="20_large_potential_gain",
        description="LOTO4 + LOTO5 avec mises élevées (gain max très grand)",
        lines=[
            LineSpec("HT_LOTO4", "LOTTO4_PATTERN", "1234", 1, stake="5.00"),
            LineSpec("HT_LOTO5", "LOTTO5_PATTERN", "12345", 1, stake="10.00"),
        ],
    )


SCENARIOS: list[Scenario] = [
    _short_ticket(),
    _medium_ticket(),
    _long_ticket(),
    _maryaj_options(),
    _loto3_options(),
    _loto4_options(),
    _loto5_options(),
    _large_gain_ticket(),
]


# ─── Fixtures shared by all scenarios ─────────────────────────────────────

@pytest.fixture
def _bootstrap(
    super_admin_client: ApiClient,
    cashier_client: ApiClient,
    cashier_context: CashierContext,
    seed_ids: SeedIds,
) -> tuple[CashierFlow, dict, Path]:
    """Bootstrap re-run per test (prereqs are idempotent — sync_app_user, open-due, session
    current/open all skip if already done). Cheaper than refactoring the fixture scopes."""
    ensure_app_user_synced(super_admin_client)
    ensure_draws_today(super_admin_client, seed_ids)
    ctx = ensure_pos_session_open(cashier_client, cashier_context)
    flow = CashierFlow(cashier_client, ctx, stake_cents=seed_ids.stake_cents)
    flow.select_context()

    draws = flow.list_available_draws()
    open_draws = [d for d in draws if d.get("status") == "OPEN" and d.get("gameCodes")]
    assert open_draws, "no OPEN draws with configured gameCodes returned"
    draw = open_draws[0]

    artifacts = _artifact_dir()
    print(f"\n[layouts] draw={draw['channelCode']} ({draw['drawId']})")
    print(f"[layouts] artifacts → {artifacts}")
    return flow, draw, artifacts


# ─── The parameterized test ──────────────────────────────────────────────

@pytest.mark.parametrize("scenario", SCENARIOS, ids=lambda s: s.name)
def test_receipt_layout(scenario: Scenario, _bootstrap) -> None:
    flow, draw, artifacts = _bootstrap
    print(f"\n>>> [{scenario.name}] {scenario.description} ({len(scenario.lines)} lines)")

    ticket = flow.sell_lines(draw, [ls.as_payload_line() for ls in scenario.lines])
    print(f">>> sold ticketCode={ticket.ticket_code}")

    pdf_bytes = flow.print_pdf(ticket.ticket_id)
    assert pdf_bytes.startswith(b"%PDF")
    pdf_path = artifacts / f"{scenario.name}.pdf"
    pdf_path.write_bytes(pdf_bytes)

    escpos_bytes = flow.print_escpos(ticket.ticket_id)
    escpos_path = artifacts / f"{scenario.name}.escpos"
    escpos_path.write_bytes(escpos_bytes)

    print(f">>> wrote {pdf_path.name} ({len(pdf_bytes)} bytes) + {escpos_path.name} ({len(escpos_bytes)} bytes)")
