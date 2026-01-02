package com.tchalanet.server.core.draw.infra.web.ops;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsResult;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeResult;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/draws")
@RequiredArgsConstructor
// @PreAuthorize("hasAuthority('SUPER_ADMIN')") todo reactivate
@Tag(name = "Ops • Scheduler")
public class DrawCalendarOpsController {

  private final CommandBus commandBus;

  @Operation(summary = "Generate draws for a date range (ops)")
  @PostMapping("/generate")
  public GenerateDrawsForRangeResult generate(
      @RequestParam TenantId tenantId,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(defaultValue = "false") boolean dryRun,
      @RequestParam(defaultValue = "false") boolean force) {
    return commandBus.send(new GenerateDrawsForRangeCommand(tenantId, from, to, dryRun, force));
  }

  @Operation(summary = "Open due draws (ops)")
  @PostMapping("/open-due")
  public OpenDueDrawsResult openDue(
      @RequestParam(defaultValue = "2000") int limit,
      @RequestParam(defaultValue = "12") int openHorizonHours,
      @RequestParam(defaultValue = "6") int openLagHours,
      @RequestParam(defaultValue = "false") boolean dryRun) {
    return commandBus.send(
        new OpenDueDrawsCommand(Instant.now(), limit, openHorizonHours, openLagHours, dryRun));
  }

  @Operation(summary = "Close due draws (ops)")
  @PostMapping("/close-due")
  public CloseDueDrawsResult closeDue(
      @RequestParam(defaultValue = "2000") int limit,
      @RequestParam(defaultValue = "false") boolean dryRun) {
    return commandBus.send(new CloseDueDrawsCommand(Instant.now(), limit, dryRun));
  }
}
