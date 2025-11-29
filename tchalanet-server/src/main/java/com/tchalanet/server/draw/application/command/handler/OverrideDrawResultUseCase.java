package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.audit.application.port.in.LogAuditEventCommandHandler;
import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.draw.application.port.in.command.OverrideDrawResultCommandHandler;
import com.tchalanet.server.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.draw.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.draw.application.port.out.DrawWriterPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OverrideDrawResultUseCase implements OverrideDrawResultCommandHandler {

  private final DrawReaderPort drawReaderPort;
  private final DrawWriterPort drawWriterPort;
  private final DrawResultReaderPort drawResultReaderPort;
  private final DrawResultWriterPort drawResultWriterPort;
  private final LogAuditEventCommandHandler audit;

  @Override
  @TchTx
  @RequiresPermission("draw.override_result")
  public void handle(OverrideDrawResultCommand command) {
    var draw =
        drawReaderPort
            .findById(command.tenantId(), command.drawId())
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
