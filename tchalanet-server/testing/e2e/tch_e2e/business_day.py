"""Reusable business-day scenario definitions for pytest and future Locust flows.

The business-day flow is intentionally data-heavy and explicit:

* provision five isolated tenants with mixed Maryaj gratis/limits/overrides;
* select one draw per active draw channel/result slot created by the DEFAULT_HAITI_LOTTERY profile;
* sell a documented ticket basket on every selected draw;
* record manual results, apply/settle them, then assert tenant/seller/draw stats.

Keep this file free of pytest imports so Locust can reuse the same tenant, draw and sales plans.
"""
from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal
from typing import Literal

MaryajMode = Literal["implicit", "explicit", "disabled"]
MaryajGratisVariant = Literal["fixed_auto", "fixed_seller_selects", "disabled"]


@dataclass(frozen=True)
class ManualResultPlan:
    """Official result used by the happy-path scenario.

    With lot1=112, lot2=21, lot3=25:
    - bolet MATCH_1_2D selection 12 wins from lot1 last two digits
    - maryaj exact-order selection 12-21 wins
    - loto3 straight selection 112 wins
    - loto4 straight selection 2125 wins from lot2+lot3
    """

    lot1: str = "112"
    lot2: str = "21"
    lot3: str = "25"

    @property
    def bolet_win(self) -> str:
        return self.lot1[-2:]

    @property
    def maryaj_win(self) -> str:
        return f"{self.lot1[-2:]}-{self.lot2}"

    @property
    def loto3_win(self) -> str:
        return self.lot1

    @property
    def loto4_win(self) -> str:
        return f"{self.lot2}{self.lot3}"

    @property
    def loto5_lot1_lot2_win(self) -> str:
        return f"{self.lot1}{self.lot2}"

    @property
    def loto5_lot1_lot3_win(self) -> str:
        return f"{self.lot1}{self.lot3}"

    @property
    def loto5_mixed_win(self) -> str:
        return f"{self.lot1[-1:]}{self.lot2}{self.lot3}"


@dataclass(frozen=True)
class SellerTerminalPlan:
    key: str
    commission_rate: str = "10.00"
    bolet_override_odds: str | None = None

    @property
    def has_override(self) -> bool:
        return self.bolet_override_odds is not None or self.commission_rate != "10.00"


@dataclass(frozen=True)
class TenantScenarioPlan:
    key: str
    maryaj_mode: MaryajMode
    maryaj_variant: MaryajGratisVariant
    seller_terminals: tuple[SellerTerminalPlan, ...]
    draw_channel_codes: tuple[str, ...] = ()
    blocked_selection: str | None = None
    draw_channel_exposure_limit_cents: int | None = None
    maryaj_gratis_fixed_amount: str = "500.00"

    @property
    def maryaj_gratis_expected(self) -> bool:
        return self.maryaj_variant in {"fixed_auto", "fixed_seller_selects"}

    @property
    def maryaj_seller_selects_expected(self) -> bool:
        return self.maryaj_variant == "fixed_seller_selects"


@dataclass(frozen=True)
class TicketScenario:
    key: str
    description: str
    paid_stake_cents: int
    lines: tuple[dict, ...]
    expected_win_cents: int = 0
    requires_maryaj_gratis: bool = False
    seller_selects_promo_selection: bool = False

    @property
    def expected_total_amount(self) -> Decimal:
        return Decimal(self.paid_stake_cents) / Decimal(100)

    @property
    def expected_winnings(self) -> Decimal:
        return Decimal(self.expected_win_cents) / Decimal(100)

    @property
    def expected_promotion_lines(self) -> int:
        return 1 if self.requires_maryaj_gratis else 0

    @property
    def auto_generated_promo_winning_ceiling(self) -> Decimal:
        if self.requires_maryaj_gratis and not self.seller_selects_promo_selection:
            return Decimal("500.00")
        return Decimal("0.00")


def default_tenant_plans() -> tuple[TenantScenarioPlan, ...]:
    """Five mixed tenants: each has multiple sellers; selected tenants combine promos/limits/overrides."""

    return (
        TenantScenarioPlan(
            key="alpha",
            maryaj_mode="implicit",
            maryaj_variant="fixed_auto",
            seller_terminals=(
                SellerTerminalPlan("main"),
                SellerTerminalPlan("override", commission_rate="17.50", bolet_override_odds="65.0000"),
            ),
            blocked_selection="44",
            draw_channel_exposure_limit_cents=100_000,
        ),
        TenantScenarioPlan(
            key="beta",
            maryaj_mode="explicit",
            maryaj_variant="fixed_auto",
            seller_terminals=(
                SellerTerminalPlan("main"),
                SellerTerminalPlan("commission", commission_rate="15.00"),
            ),
        ),
        TenantScenarioPlan(
            key="gamma",
            maryaj_mode="disabled",
            maryaj_variant="disabled",
            seller_terminals=(
                SellerTerminalPlan("main"),
                SellerTerminalPlan("backup"),
            ),
            blocked_selection="44",
        ),
        TenantScenarioPlan(
            key="delta",
            maryaj_mode="implicit",
            maryaj_variant="fixed_seller_selects",
            seller_terminals=(
                SellerTerminalPlan("main"),
                SellerTerminalPlan("override", commission_rate="12.50", bolet_override_odds="65.0000"),
            ),
            draw_channel_exposure_limit_cents=100_000,
        ),
        TenantScenarioPlan(
            key="epsilon",
            maryaj_mode="explicit",
            maryaj_variant="fixed_auto",
            seller_terminals=(
                SellerTerminalPlan("main"),
                SellerTerminalPlan("backup"),
            ),
        ),
    )


def ticket_basket(
    result: ManualResultPlan,
    seller: SellerTerminalPlan,
    plan: TenantScenarioPlan,
) -> tuple[TicketScenario, ...]:
    """Documented basket sold on every selected draw.

    The first tickets are named business cases; the tail adds non-winning volume so dashboards,
    draw reports and seller reports have enough rows to validate stable aggregates.
    """

    scenarios = [
        short_ticket(),
        maryaj_gratis_ticket(result, plan),
        winning_lot1_only_ticket(result, seller),
        winning_lot1_lot2_ticket(result),
        winning_lot1_lot3_ticket(result),
        winning_lot1_lot2_lot3_ticket(result),
    ]
    scenarios.extend(volume_ticket(i) for i in range(6))
    return tuple(scenarios)


def ticket_scenario(
    index: int,
    result: ManualResultPlan,
    seller: SellerTerminalPlan | None = None,
    includes_maryaj_gratis: bool = False,
) -> TicketScenario:
    """Compatibility helper: return the indexed scenario from the default basket."""

    seller_plan = seller or SellerTerminalPlan("main")
    plan = TenantScenarioPlan(
        key="compat",
        maryaj_mode="implicit" if includes_maryaj_gratis else "disabled",
        maryaj_variant="fixed_auto" if includes_maryaj_gratis else "disabled",
        seller_terminals=(seller_plan,),
    )
    basket = ticket_basket(result, seller_plan, plan)
    return basket[index % len(basket)]


def short_ticket() -> TicketScenario:
    lines = (line(1, "HT_BOLET", "MATCH_1_2D", "17", None, 100),)
    return scenario("short", "Ticket court non-gagnant: one Bolet line.", lines)


def exposure_limit_probe_ticket() -> TicketScenario:
    lines = (line(1, "HT_BOLET", "MATCH_1_2D", "99", None, 200),)
    return scenario(
        "exposure-limit-probe",
        "Dedicated negative exposure ticket; selection 99 is absent from the happy-path basket.",
        lines,
    )


def maryaj_gratis_ticket(result: ManualResultPlan, plan: TenantScenarioPlan) -> TicketScenario:
    lines = (
        line(1, "HT_BOLET", "MATCH_1_2D", "18", None, 100),
        line(2, "HT_MARYAJ", "MARRIAGE_2D2D", result.maryaj_win, 1, 100),
    )
    expected = 100_000
    # Seller-selects pins the free Maryaj selection to the winning numbers; auto-generated lines
    # validate promo generation but are not deterministic winning selections.
    if plan.maryaj_seller_selects_expected:
        expected += int(Decimal(plan.maryaj_gratis_fixed_amount) * Decimal(100))
    return scenario(
        f"maryaj-{plan.maryaj_variant}",
        "Maryaj winning ticket; variant controls free-line fixed payout, boost odds, or disabled state.",
        lines,
        expected_win_cents=expected,
        requires_maryaj_gratis=plan.maryaj_gratis_expected,
        seller_selects_promo_selection=plan.maryaj_seller_selects_expected,
    )


def winning_lot1_only_ticket(result: ManualResultPlan, seller: SellerTerminalPlan) -> TicketScenario:
    lines = (line(1, "HT_BOLET", "MATCH_1_2D", result.bolet_win, None, 100),)
    multiplier = Decimal(seller.bolet_override_odds or "50.0000")
    return scenario(
        "win-lot1-only",
        "Bolet gagnant sur lot1 only; validates seller-terminal odds override.",
        lines,
        expected_win_cents=int(multiplier * Decimal(100)),
    )


def winning_lot1_lot2_ticket(result: ManualResultPlan) -> TicketScenario:
    lines = (line(1, "HT_LOTO5", "LOTTO5_PATTERN", result.loto5_lot1_lot2_win, 1, 100),)
    return scenario(
        "win-lot1-lot2",
        "Loto5 option 1: lot1 + lot2.",
        lines,
        expected_win_cents=2_500_000,
    )


def winning_lot1_lot3_ticket(result: ManualResultPlan) -> TicketScenario:
    lines = (line(1, "HT_LOTO5", "LOTTO5_PATTERN", result.loto5_lot1_lot3_win, 2, 100),)
    return scenario(
        "win-lot1-lot3",
        "Loto5 option 2: lot1 + lot3.",
        lines,
        expected_win_cents=2_500_000,
    )


def winning_lot1_lot2_lot3_ticket(result: ManualResultPlan) -> TicketScenario:
    lines = (
        line(1, "HT_LOTO3", "LOTTO3_3D", result.loto3_win, 1, 100),
        line(2, "HT_LOTO4", "LOTTO4_PATTERN", result.loto4_win, 1, 100),
        line(3, "HT_LOTO5", "LOTTO5_PATTERN", result.loto5_mixed_win, 3, 100),
    )
    return scenario(
        "win-lot1-lot2-lot3",
        "Loto3 straight + Loto4 straight + Loto5 mixed 1/2/3.",
        lines,
        expected_win_cents=50_000 + 500_000 + 2_500_000,
    )


def volume_ticket(index: int) -> TicketScenario:
    first = 10 + ((index * 7) % 80)
    if first == 44:
        first = 45
    lines = (
        line(1, "HT_BOLET", "MATCH_1_2D", f"{first:02d}", None, 100),
        line(2, "HT_MARYAJ", "MARRIAGE_2D2D", "30-31", 1, 100),
        line(3, "HT_LOTO3", "LOTTO3_3D", f"{200 + index:03d}"[-3:], 1, 100),
    )
    return scenario(
        f"volume-nonwinner-{index:02d}",
        "Non-winning volume ticket; avoids blocked selection 44.",
        lines,
    )


def scenario(
    key: str,
    description: str,
    lines: tuple[dict, ...],
    expected_win_cents: int = 0,
    requires_maryaj_gratis: bool = False,
    seller_selects_promo_selection: bool = False,
) -> TicketScenario:
    return TicketScenario(
        key=key,
        description=description,
        paid_stake_cents=sum(int(Decimal(str(item["stakeAmount"])) * 100) for item in lines),
        lines=lines,
        expected_win_cents=expected_win_cents,
        requires_maryaj_gratis=requires_maryaj_gratis,
        seller_selects_promo_selection=seller_selects_promo_selection,
    )


def line(
    line_number: int,
    game_code: str,
    bet_type: str,
    selection: str,
    bet_option: int | None,
    stake_cents: int,
) -> dict:
    payload = {
        "lineNumber": line_number,
        "gameCode": game_code,
        "betType": bet_type,
        "selection": selection,
        "stakeAmount": f"{Decimal(stake_cents) / Decimal(100):.2f}",
    }
    if bet_option is not None:
        payload["betOption"] = bet_option
    return payload


def sale_payload(draw: dict, scenario: TicketScenario) -> dict:
    return {
        "drawId": draw_id(draw),
        "drawChannelId": draw_channel_id(draw),
        "currency": {"value": "HTG"},
        "lines": list(scenario.lines),
    }


def draw_id(draw: dict) -> str:
    value = draw.get("id") or draw.get("drawId")
    if isinstance(value, dict):
        return str(value.get("value") or value.get("id"))
    return str(value)


def draw_channel_id(draw: dict) -> str:
    if "drawChannelId" in draw:
        return str(draw["drawChannelId"])
    channel = draw.get("channel") or {}
    value = channel.get("id")
    if isinstance(value, dict):
        return str(value.get("value") or value.get("id"))
    return str(value)
