package com.tchalanet.server.features.ops.infra.web.draws;

import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.draw.application.command.model.*;
import com.tchalanet.server.features.ops.infra.web.draws.model.CloseDueDrawsRequest;
import com.tchalanet.server.features.ops.infra.web.draws.model.GenerateDrawsRequest;
import com.tchalanet.server.features.ops.infra.web.draws.model.OpenDueDrawsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/platform/ops/draws")
@RequiredArgsConstructor
// @PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Scheduler")
public class DrawCalendarOpsController {

    private final CommandBus commandBus;
    private final BatchGate batchGate;

    @Operation(summary = "Generate draws for a date range (ops)")
    @PostMapping("/generate")
    public GenerateDrawsForRangeResult generate(@Valid @RequestBody GenerateDrawsRequest req) {
        batchGate.assertEnabledOrThrow(BatchJobKeys.DRAW_GENERATE, TenantId.parse(req.tenantId()));
        return commandBus.send(
            new GenerateDrawsForRangeCommand(
                TenantId.parse(req.tenantId()), req.from(), req.to(), req.dryRun(), req.force()));
    }

    @Operation(summary = "Open due draws (ops)")
    @PostMapping("/open-due")
    public OpenDueDrawsResult openDue(@Valid @RequestBody OpenDueDrawsRequest req) {
        batchGate.assertEnabledOrThrow(BatchJobKeys.DRAW_OPEN, null);
        Instant now = req.now() == null ? Instant.now() : req.now();
        return commandBus.send(
            new OpenDueDrawsCommand(
                now, req.limit(), req.openHorizonHours(), req.openLagHours(), req.dryRun()));
    }

    @Operation(summary = "Close due draws (ops)")
    @PostMapping("/close-due")
    public CloseDueDrawsResult closeDue(@Valid @RequestBody CloseDueDrawsRequest req) {
        batchGate.assertEnabledOrThrow(BatchJobKeys.DRAW_CLOSE, null);
        Instant now = req.now() == null ? Instant.now() : req.now();
        return commandBus.send(new CloseDueDrawsCommand(now, req.limit(), req.dryRun()));
    }

    @Operation(summary = "Apply draw_result to draw.draw_result_id (slot-first)")
    @PostMapping("/apply")
    public ApiResponse<ApplyExternalResultsWindowResult> apply(
        @RequestBody DrawResultsOpsController.WindowRequest req) {
        var tenantId = TenantId.parse(req.tenantId());
        batchGate.assertEnabledOrThrow(BatchJobKeys.RESULTS_EXTERNAL_APPLY, tenantId);
        var res =
            commandBus.send(
                new ApplyExternalResultsWindowCommand(
                    tenantId,
                    req.baseDate(),
                    req.daysBack(),
                    req.slotKeys(),
                    req.force(),
                    req.dryRun(),
                    req.maxSlots()));
        return ApiResponse.success(res);
    }
}
