"""Build a randomized 5–10 line ticket basket, reusing tch_e2e.ticket_matrix."""
from __future__ import annotations

import random
from typing import Any

from tch_e2e.ticket_matrix import LineSpec, TicketScenario

_TWO_DIGIT = [f"{n:02d}" for n in range(100)]


def random_basket(min_lines: int = 5, max_lines: int = 10, stake_cents: int = 100) -> list[dict[str, Any]]:
    """A basket of ``min_lines``..``max_lines`` HT_BOLET 2-digit lines (currently-supported bets)."""
    lo, hi = sorted((max(1, min_lines), max(1, max_lines)))
    n = random.randint(lo, hi)
    lines = tuple(
        LineSpec(
            game_code="HT_BOLET",
            bet_type="MATCH_1_2D",
            selection=random.choice(_TWO_DIGIT),
            bet_option=None,
            stake_cents=stake_cents,
        )
        for _ in range(n)
    )
    return TicketScenario(
        name="load-basket",
        description=f"Randomized {n}-line load basket",
        lines=lines,
    ).to_payload_lines()
