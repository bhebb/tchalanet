package com.tchalanet.server.features.ops.infra.web.draws;

import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowResult;
import com.tchalanet.server.core.drawresult.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.drawresult.application.command.model.OverrideDrawResultResult;
import com.tchalanet.server.core.drawresult.application.command.model.RecordManualDrawResultCommand;
import com.tchalanet.server.core.drawresult.application.command.model.RecordManualDrawResultResult;
import com.tchalanet.server.core.drawresult.application.command.model.RefreshExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.application.command.model.RefreshExternalResultsWindowResult;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/platform/ops/draw-results")
@RequiredArgsConstructor
@Tag(name = "Ops • Draw Results")
public class DrawResultsOpsController {

  private final CommandBus commandBus;
  private final BatchGate gate;

  @Operation(summary = "Fetch external results into draw_result (slot-first)")
  @PostMapping("/fetch")
  public ApiResponse<FetchExternalResultsWindowResult> fetch(@RequestBody WindowRequest req) {
    if (!gate.enabled(BatchJobKeys.RESULTS_EXTERNAL_FETCH, tenant(req.tenantId()))) {
      throw new IllegalStateException("Batch gate: results.fetch=false");
    }
    var res =
        commandBus.send(
            new FetchExternalResultsWindowCommand(
                tenant(req.tenantId()),
                nnDate(req.baseDate()),
                nnInt(req.daysBack(), 0),
                normalize(req.slotKeys()),
                req.force(),
                req.dryRun(),
                nnInt(req.maxSlots(), 200)));
    return ApiResponse.success(res);
  }

  @Operation(summary = "Refresh (fetch then apply) — orchestrator only")
  @PostMapping("/refresh")
  public ApiResponse<RefreshExternalResultsWindowResult> refresh(@RequestBody WindowRequest req) {
    if (!gate.enabled(BatchJobKeys.RESULTS_EXTERNAL_REFRESH, tenant(req.tenantId())))
      throw new IllegalStateException("Batch gate: results.refresh=false");
    TenantId t = tenant(req.tenantId());
    if (t == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId required");
    var res =
        commandBus.send(
            new RefreshExternalResultsWindowCommand(
                t,
                nnDate(req.baseDate()),
                nnInt(req.daysBack(), 0),
                normalize(req.slotKeys()),
                req.force(),
                req.dryRun(),
                nnInt(req.maxSlots(), 200)));
    return ApiResponse.success(res);
  }

  @Operation(summary = "Override a draw result for a slot (OPS)")
  @PostMapping("/override")
  public ApiResponse<OverrideDrawResultResult> override(@RequestBody OverrideRequest req) {
    gate.assertEnabledOrThrow(BatchJobKeys.RESULTS_EXTERNAL_REFRESH, tenant(req.tenantId()));
    TenantId t = tenant(req.tenantId());
    // tenant may be null for platform-level overrides depending on design; require it here for
    // safety
    if (t == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId required");

    var cmd =
        new OverrideDrawResultCommand(
            t,
            normalizeSlot(req.slotKey()),
            req.drawDate(),
            req.pick3(),
            req.pick4(),
            req.reason(),
            req.force());

    var res = commandBus.send(cmd);
    return ApiResponse.success(res);
  }

  @Operation(summary = "Record a manual draw result (OPS)")
  @PostMapping("/manual")
  public ApiResponse<RecordManualDrawResultResult> manual(@RequestBody ManualRequest req) {
    gate.assertEnabledOrThrow(BatchJobKeys.RESULTS_EXTERNAL_REFRESH, tenant(req.tenantId()));
    TenantId t = tenant(req.tenantId());
    if (t == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId required");

    var cmd =
        new RecordManualDrawResultCommand(
            t,
            req.drawDate(),
            normalizeSlot(req.slotKey()),
            req.recordedBy(),
            req.notes(),
            req.pick3(),
            req.pick4(),
            req.force());

    var res = commandBus.send(cmd);
    return ApiResponse.success(res);
  }

  private static TenantId tenant(String id) {
    return (id == null || id.isBlank()) ? null : TenantId.parse(id);
  }

  private static LocalDate nnDate(LocalDate d) {
    return d == null ? LocalDate.now() : d;
  }

  private static int nnInt(Integer v, int def) {
    return v == null ? def : v;
  }

  private static List<String> normalize(List<String> in) {
    if (in == null) return List.of();
    return in.stream()
        .filter(Objects::nonNull)
        .map(s -> s.trim().toUpperCase(Locale.ROOT))
        .filter(s -> !s.isBlank())
        .distinct()
        .toList();
  }

  private static String normalizeSlot(String s) {
    return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
  }

  public record WindowRequest(
      String tenantId,
      LocalDate baseDate,
      Integer daysBack,
      List<String> slotKeys,
      boolean force,
      boolean dryRun,
      Integer maxSlots) {}

  public record OverrideRequest(
      String tenantId,
      String slotKey,
      LocalDate drawDate,
      String pick3,
      String pick4,
      String reason,
      boolean force) {}

  public record ManualRequest(
      String tenantId,
      LocalDate drawDate,
      String slotKey,
      String recordedBy,
      String notes,
      String pick3,
      String pick4,
      boolean force) {}
}
