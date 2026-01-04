package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.draw.application.command.model.CancelDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
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
public class CancelDrawsCommandHandler implements VoidCommandHandler<CancelDrawCommand> {

  private final DrawReaderPort drawReaderPort;
  private final DrawLifecyclePort drawWriterPort;

  // private final DrawSettlementPort settlementPort; // vers sales/ledger

  @Override
  @TchTx
  @RequiresPermission("draw.settle")
  @AuditLog(
      entity = AuditEntityType.DRAW,
      action = AuditAction.SETTLE,
      idExpression = "#command.drawId.toString()")
  public void handle(CancelDrawCommand command) {
    var draw =
        drawReaderPort
            .findById(command.drawId())
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + command.drawId()));

    // TODO: appeler les autres BC (tickets, odds, ledger) via un port out
    // settlementPort.settleDraw(command.tenantId(), draw, result);

    draw.cancel(command.reason());
    drawWriterPort.save(draw);
  }
}
