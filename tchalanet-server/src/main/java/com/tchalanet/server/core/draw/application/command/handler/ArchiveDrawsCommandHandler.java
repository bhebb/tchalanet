package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.draw.application.command.model.ArchiveDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Use case pour archiver un tirage. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class ArchiveDrawsCommandHandler implements VoidCommandHandler<ArchiveDrawCommand> {

  private final DrawReaderPort drawReaderPort;
  private final DrawResultReaderPort drawResultReaderPort;
  private final DrawWriterPort drawWriterPort;

  // private final DrawSettlementPort settlementPort; // vers sales/ledger

  @Override
  @TchTx
  @RequiresPermission("draw.archive")
  @AuditLog(
      entity = AuditEntityType.DRAW,
      action = AuditAction.ARCHIVE,
      idExpression = "#command.drawId.toString()")
  public void handle(ArchiveDrawCommand command) {
    var draw =
        drawReaderPort
            .findById(command.drawId())
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + command.drawId()));

    var result =
        drawResultReaderPort
            .findByDrawId(command.tenantId(), command.drawId())
            .orElseThrow(() -> new IllegalStateException("Cannot archive without result"));

    // TODO: appeler les autres BC (tickets, odds, ledger) via un port out
    draw.archive();
    drawWriterPort.save(draw);
  }
}
