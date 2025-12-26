package com.tchalanet.server.core.ledger.domain.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LedgerEntryFactory {

  public static LedgerEntry createDeposit(
      TenantId tenantId, UUID refId, BigDecimal amount, Instant occurredAt) {
    validate(amount);
    return LedgerEntry.create(
        tenantId, LedgerRefType.CASH_DEPOSIT, refId, amount, LedgerDirection.CREDIT, occurredAt);
  }

  public static LedgerEntry createWithdraw(
      TenantId tenantId, UUID refId, BigDecimal amount, Instant occurredAt) {
    validate(amount);
    return LedgerEntry.create(
        tenantId, LedgerRefType.CASH_WITHDRAW, refId, amount, LedgerDirection.DEBIT, occurredAt);
  }

  public static LedgerEntry createReversal(LedgerEntry original, Instant occurredAt) {
    var inverseDirection =
        original.direction() == LedgerDirection.DEBIT
            ? LedgerDirection.CREDIT
            : LedgerDirection.DEBIT;
    return LedgerEntry.create(
        original.tenantId(),
        LedgerRefType.REVERSAL,
        original.refId(),
        original.amount(),
        inverseDirection,
        occurredAt);
  }

  private static void validate(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    if (amount.scale() > 2) {
      throw new IllegalArgumentException("Amount scale cannot exceed 2");
    }
  }
}
