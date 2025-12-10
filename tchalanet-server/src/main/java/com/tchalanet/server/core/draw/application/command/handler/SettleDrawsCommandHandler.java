package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.audit.domain.model.AuditAction;
import com.tchalanet.server.core.audit.domain.model.AuditEntityType;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case pour régler un tirage : - charger les tickets, - calculer gains/pertes/commissions, -
 * persister les mouvements et soldes, - marquer le tirage comme SETTLED, - gérer les invalidations
 * / refresh caches.
 *
 * <p>TODO: brancher les ports out (TicketReader/WriterPort, DrawWriterPort, etc.) et implémenter la
 * logique métier.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class SettleDrawsCommandHandler implements VoidCommandHandler<SettleDrawCommand> {

  private final DrawReaderPort drawReaderPort;
  private final DrawResultReaderPort drawResultReaderPort;
  private final DrawWriterPort drawWriterPort;

  // private final DrawSettlementPort settlementPort; // vers sales/ledger

  @Override
  @TchTx
  @RequiresPermission("draw.settle")
  @AuditLog(
      entity = AuditEntityType.DRAW,
      action = AuditAction.SETTLE,
      idExpression = "#command.drawId.toString()")
  public void handle(SettleDrawCommand command) {
    var draw =
        drawReaderPort
            .findById(command.tenantId(), command.drawId())
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + command.drawId()));

    var result =
        drawResultReaderPort
            .findByDrawId(command.tenantId(), command.drawId())
            .orElseThrow(() -> new IllegalStateException("Cannot settle without result"));

    // TODO: appeler les autres BC (tickets, odds, ledger) via un port out
    // settlementPort.settleDraw(command.tenantId(), draw, result);

    draw.settle();
    drawWriterPort.save(draw);
  }
}
