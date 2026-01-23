package com.tchalanet.server.common.types.id;

import java.util.UUID;

/**
 * Utility for creating random ID wrappers in tests.
 *
 * <p>MUST NOT be used in production code.
 */
public final class TestIds {
  private TestIds() {}

  public static TenantId tenant() {
    return TenantId.of(UUID.randomUUID());
  }

  public static TicketId ticket() {
    return TicketId.of(UUID.randomUUID());
  }

  public static PayoutId payout() {
    return PayoutId.of(UUID.randomUUID());
  }

  public static UserId user() {
    return UserId.of(UUID.randomUUID());
  }

  public static AgentId agent() {
    return AgentId.of(UUID.randomUUID());
  }

  public static DrawId draw() {
    return DrawId.of(UUID.randomUUID());
  }

  public static GameId game() {
    return GameId.of(UUID.randomUUID());
  }

  public static OutletId outlet() {
    return OutletId.of(UUID.randomUUID());
  }

  public static PlanId plan() {
    return PlanId.of(UUID.randomUUID());
  }

  public static TerminalId terminal() {
    return TerminalId.of(UUID.randomUUID());
  }

  public static SessionId session() {
    return SessionId.of(UUID.randomUUID());
  }

  public static RoleId role() {
    return RoleId.of(UUID.randomUUID());
  }

  public static SubscriptionId subscription() {
    return SubscriptionId.of(UUID.randomUUID());
  }

  public static AddressId address() {
    return AddressId.of(UUID.randomUUID());
  }

  public static TenantGameId tenantGame() {
    return TenantGameId.of(UUID.randomUUID());
  }

  public static DrawChannelId drawChannel() {
    return DrawChannelId.of(UUID.randomUUID());
  }

  public static DrawResultId drawResult() {
    return DrawResultId.of(UUID.randomUUID());
  }

  public static TchalaEntryId tchalaEntry() {
    return TchalaEntryId.of(UUID.randomUUID());
  }

  public static ResultSlotId resultSlot() {
    return ResultSlotId.of(UUID.randomUUID());
  }
}
