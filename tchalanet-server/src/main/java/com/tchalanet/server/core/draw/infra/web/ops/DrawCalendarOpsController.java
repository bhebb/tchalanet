package com.tchalanet.server.core.draw.infra.web.ops;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsResult;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeCommand;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForRangeResult;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.infra.web.ops.model.CloseDueDrawsRequest;
import com.tchalanet.server.core.draw.infra.web.ops.model.GenerateDrawsRequest;
import com.tchalanet.server.core.draw.infra.web.ops.model.OpenDueDrawsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/draws")
@RequiredArgsConstructor
// @PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Scheduler")
public class DrawCalendarOpsController {

  private final CommandBus commandBus;
  private final com.tchalanet.server.common.batch.BatchGate batchGate;

  @Operation(summary = "Generate draws for a date range (ops)")
  @PostMapping("/generate")
  public GenerateDrawsForRangeResult generate(@Valid @RequestBody GenerateDrawsRequest req) {
    batchGate.assertCanRunOrThrow("generate");
    return commandBus.send(
        new GenerateDrawsForRangeCommand(
            TenantId.of(req.tenantId()), req.from(), req.to(), req.dryRun(), req.force()));
  }

  @Operation(summary = "Open due draws (ops)")
  @PostMapping("/open-due")
  public OpenDueDrawsResult openDue(@Valid @RequestBody OpenDueDrawsRequest req) {
    batchGate.assertCanRunOrThrow("open");
    Instant now = req.now() == null ? Instant.now() : req.now();
    return commandBus.send(
        new OpenDueDrawsCommand(
            now, req.limit(), req.openHorizonHours(), req.openLagHours(), req.dryRun()));
  }

  @Operation(summary = "Close due draws (ops)")
  @PostMapping("/close-due")
  public CloseDueDrawsResult closeDue(@Valid @RequestBody CloseDueDrawsRequest req) {
    batchGate.assertCanRunOrThrow("close");
    Instant now = req.now() == null ? Instant.now() : req.now();
    return commandBus.send(new CloseDueDrawsCommand(now, req.limit(), req.dryRun()));
  }
}
