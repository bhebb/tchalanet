"""Ticket scenario matrix — 9 scenarios from design.md §5."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class LineSpec:
    game_code: str
    bet_type: str
    selection: str
    bet_option: int | None
    stake_cents: int


@dataclass(frozen=True)
class TicketScenario:
    name: str
    lines: tuple[LineSpec, ...]
    description: str = ""

    def to_payload_lines(self) -> list[dict[str, Any]]:
        return [
            {
                "gameCode": ls.game_code,
                "betType": ls.bet_type,
                "selection": ls.selection,
                "betOption": ls.bet_option,
                "stake": f"{ls.stake_cents / 100:.2f}",
            }
            for ls in self.lines
        ]


# ---------------------------------------------------------------------------
# Implemented scenarios
# ---------------------------------------------------------------------------

SHORT_SINGLE_GAME_LOW_STAKE = TicketScenario(
    name="SHORT_SINGLE_GAME_LOW_STAKE",
    description="Smoke sell — 1 game, 1 line, low stake.",
    lines=(
        LineSpec(game_code="HT_BOLET", bet_type="MATCH_1_2D", selection="42", bet_option=None, stake_cents=100),
    ),
)

SHORT_SINGLE_GAME_HIGH_STAKE = TicketScenario(
    name="SHORT_SINGLE_GAME_HIGH_STAKE",
    description="Limit / line-block test — 1 game, 1 line, high stake.",
    lines=(
        LineSpec(game_code="HT_BOLET", bet_type="MATCH_1_2D", selection="77", bet_option=None, stake_cents=50000),
    ),
)

# ---------------------------------------------------------------------------
# Implemented V1 scenarios
# ---------------------------------------------------------------------------

MEDIUM_MULTI_LINE_MIXED_STAKE = TicketScenario(
    name="MEDIUM_MULTI_LINE_MIXED_STAKE",
    description="Medium ticket — 2 games, 4 lines, mixed stakes.",
    lines=(
        LineSpec(game_code="HT_BOLET", bet_type="MATCH_1_2D", selection="11", bet_option=None, stake_cents=100),
        LineSpec(game_code="HT_BOLET", bet_type="MATCH_1_2D", selection="22", bet_option=None, stake_cents=200),
        LineSpec(game_code="HT_MARYAJ", bet_type="MARRIAGE_2D2D", selection="21-25", bet_option=1, stake_cents=100),
        LineSpec(game_code="HT_MARYAJ", bet_type="MARRIAGE_2D2D", selection="33-77", bet_option=1, stake_cents=200),
    ),
)

MEDIUM_MULTI_GAME = TicketScenario(
    name="MEDIUM_MULTI_GAME",
    description="Medium ticket — 3 games, 5 lines.",
    lines=(
        LineSpec(game_code="HT_BOLET",  bet_type="MATCH_1_2D",    selection="55",    bet_option=None, stake_cents=100),
        LineSpec(game_code="HT_BOLET",  bet_type="MATCH_1_2D",    selection="66",    bet_option=None, stake_cents=100),
        LineSpec(game_code="HT_MARYAJ", bet_type="MARRIAGE_2D2D", selection="11-99", bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_LOTO3",  bet_type="LOTTO3_3D",     selection="777",   bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_LOTO3",  bet_type="LOTTO3_3D",     selection="888",   bet_option=1,    stake_cents=100),
    ),
)

LONG_ALL_GAMES = TicketScenario(
    name="LONG_ALL_GAMES",
    description="Long ticket — all 5 V1 games, 17 lines, mixed stakes (design §5: 8-20 lines).",
    lines=(
        # HT_BOLET — 5 lines
        LineSpec(game_code="HT_BOLET",  bet_type="MATCH_1_2D",      selection="10",    bet_option=None, stake_cents=100),
        LineSpec(game_code="HT_BOLET",  bet_type="MATCH_1_2D",      selection="20",    bet_option=None, stake_cents=100),
        LineSpec(game_code="HT_BOLET",  bet_type="MATCH_1_2D",      selection="30",    bet_option=None, stake_cents=200),
        LineSpec(game_code="HT_BOLET",  bet_type="MATCH_1_2D",      selection="40",    bet_option=None, stake_cents=200),
        LineSpec(game_code="HT_BOLET",  bet_type="MATCH_1_2D",      selection="50",    bet_option=None, stake_cents=500),
        # HT_MARYAJ — 3 lines
        LineSpec(game_code="HT_MARYAJ", bet_type="MARRIAGE_2D2D",   selection="12-34", bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_MARYAJ", bet_type="MARRIAGE_2D2D",   selection="56-78", bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_MARYAJ", bet_type="MARRIAGE_2D2D",   selection="13-57", bet_option=1,    stake_cents=200),
        # HT_LOTO3 — 3 lines
        LineSpec(game_code="HT_LOTO3",  bet_type="LOTTO3_3D",       selection="111",   bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_LOTO3",  bet_type="LOTTO3_3D",       selection="222",   bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_LOTO3",  bet_type="LOTTO3_3D",       selection="333",   bet_option=1,    stake_cents=200),
        # HT_LOTO4 — 3 lines
        LineSpec(game_code="HT_LOTO4",  bet_type="LOTTO4_PATTERN",  selection="1111",  bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_LOTO4",  bet_type="LOTTO4_PATTERN",  selection="2222",  bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_LOTO4",  bet_type="LOTTO4_PATTERN",  selection="3333",  bet_option=1,    stake_cents=200),
        # HT_LOTO5 — 3 lines
        LineSpec(game_code="HT_LOTO5",  bet_type="LOTTO5_PATTERN",  selection="11111", bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_LOTO5",  bet_type="LOTTO5_PATTERN",  selection="22222", bet_option=1,    stake_cents=100),
        LineSpec(game_code="HT_LOTO5",  bet_type="LOTTO5_PATTERN",  selection="33333", bet_option=1,    stake_cents=200),
    ),
)

# ---------------------------------------------------------------------------
# Stub scenarios — implement when limits/promotions suite is built
# ---------------------------------------------------------------------------

def _not_implemented(name: str) -> TicketScenario:
    raise NotImplementedError(
        f"TicketScenario {name!r} is not yet implemented. "
        "Build it when the corresponding test suite is added."
    )


def limit_exposure_race() -> TicketScenario:
    return _not_implemented("LIMIT_EXPOSURE_RACE")


def promo_boost_odds() -> TicketScenario:
    return _not_implemented("PROMO_BOOST_ODDS")


def promo_free_game_line() -> TicketScenario:
    return _not_implemented("PROMO_FREE_GAME_LINE")


def promo_waive_charge() -> TicketScenario:
    return _not_implemented("PROMO_WAIVE_CHARGE")
