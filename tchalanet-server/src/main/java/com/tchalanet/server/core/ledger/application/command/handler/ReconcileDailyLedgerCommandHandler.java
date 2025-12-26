package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.command.model.ReconcileDailyLedgerCommand;
import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@UseCase
@Component
@RequiredArgsConstructor
@Slf4j
public class ReconcileDailyLedgerCommandHandler
    implements VoidCommandHandler<ReconcileDailyLedgerCommand> {

  private final LedgerReaderPort ledgerReader;

  @Override
  public void handle(ReconcileDailyLedgerCommand command) {
    try {
      var entries =
          ledgerReader.findByTenant(
              command.tenantId(), command.dayStart(), command.dayEnd(), Integer.MAX_VALUE, 0);
      var totalDebit =
          entries.stream()
              .filter(e -> e.direction() == LedgerDirection.DEBIT)
              .map(LedgerEntry::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      var totalCredit =
          entries.stream()
              .filter(e -> e.direction() == LedgerDirection.CREDIT)
              .map(LedgerEntry::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      var balance = totalCredit.subtract(totalDebit);
      log.info(
          "Reconciled ledger for tenant {}: {} entries, debit={}, credit={}, balance={}",
          command.tenantId(),
          entries.size(),
          totalDebit,
          totalCredit,
          balance);
    } catch (Exception e) {
      log.error("Failed to reconcile ledger for tenant {}", command.tenantId(), e);
    }
  }
}
