package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.audit.application.command.handler.AuditLoggingCommandHandler;
import com.tchalanet.server.core.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.core.draw.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OverrideDrawResultCommandHandler
    implements VoidCommandHandler<OverrideDrawResultCommand> {

  private final DrawReaderPort drawReaderPort;
  private final DrawLifecyclePort drawWriterPort;
  private final DrawResultReaderPort drawResultReaderPort;
  private final DrawResultWriterPort drawResultWriterPort;
  private final AuditLoggingCommandHandler audit;

  @Override
  @TchTx
  @RequiresPermission("draw.override_result")
  public void handle(OverrideDrawResultCommand command) {
    var draw =
        drawReaderPort
            .findById(command.drawId())
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + command.drawId()));

    var currentResult =
        drawResultReaderPort
            .findByDrawId(command.tenantId(), command.drawId())
            .orElseThrow(() -> new IllegalStateException("No existing result to override"));

    var overridden =
        currentResult.override(command.numbersMain(), command.numbersExtra(), command.reason());

    // On applique la nouvelle valeur au domaine
    draw.applyResult(overridden);

    drawResultWriterPort.save(command.tenantId(), command.drawId(), overridden);
    drawWriterPort.save(draw);

    // 5. Audit the manual override (audit is already handled by ApplyDrawResultService, but this is
    // specific to override)
    var details = Map.<String, Object>of("reason", "admin_override");
    audit.handle(
        new LogAuditEventCommand(
            AuditEntityType.DRAW, draw.id().toString(), AuditAction.UPDATE, details));

    log.info("Successfully overrode result for draw {}", draw.id().toString());
  }
}
