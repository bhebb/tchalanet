package com.tchalanet.server.core.draw.infra.web.ops;

import com.tchalanet.server.common.batch.BatchGate;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsForDateCommand;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsForDateResult;
import com.tchalanet.server.core.draw.application.command.model.FetchExternalResultsForDateCommand;
import com.tchalanet.server.core.draw.application.command.model.FetchExternalResultsForDateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/ops/draw-results")
@RequiredArgsConstructor
// @PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Draw Results")
public class DrawResultsOpsController {

  private final CommandBus commandBus;
  private final BatchGate gate;

  @Operation(summary = "Fetch external results (bulk) into draw_result")
  @PostMapping("/fetch")
  public FetchExternalResultsForDateResult fetch(@RequestBody FetchRequest req) {
    if (!gate.canRun("results.fetch")) {
      throw new IllegalStateException("Batch gate: results.fetch=false");
    }

    var normalized = normalize(req.channelCodes());
    return commandBus.send(
        new FetchExternalResultsForDateCommand(
            TenantId.of(req.tenantId()),
            req.drawDate(),
            normalized,
            req.force(),
            req.dryRun(),
            req.maxDraws()));
  }

  @Operation(summary = "Apply results from draw_result to draw (bulk)")
  @PostMapping("/apply")
  public ApplyExternalResultsForDateResult apply(@RequestBody ApplyRequest req) {
    if (!gate.canRun("results.apply")) {
      throw new IllegalStateException("Batch gate: results.apply=false");
    }

    var normalized = normalize(req.channelCodes());
    return commandBus.send(
        new ApplyExternalResultsForDateCommand(
            TenantId.of(req.tenantId()),
            req.drawDate(),
            normalized,
            req.force(),
            req.dryRun(),
            req.maxDraws()));
  }

  @Operation(summary = "Refresh results (fetch then apply)")
  @PostMapping("/refresh")
  public RefreshResponse refresh(@RequestBody RefreshRequest req) {
    var fetchRes =
        fetch(
            new FetchRequest(
                req.tenantId(),
                req.drawDate(),
                req.channelCodes(),
                req.force(),
                req.dryRun(),
                req.maxDraws()));
    var applyRes =
        apply(
            new ApplyRequest(
                req.tenantId(),
                req.drawDate(),
                req.channelCodes(),
                req.force(),
                req.dryRun(),
                req.maxDraws()));
    return new RefreshResponse(fetchRes, applyRes);
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

  public record FetchRequest(
      String tenantId,
      LocalDate drawDate,
      List<String> channelCodes,
      boolean force,
      boolean dryRun,
      int maxDraws) {}

  public record ApplyRequest(
      String tenantId,
      LocalDate drawDate,
      List<String> channelCodes,
      boolean force,
      boolean dryRun,
      int maxDraws) {}

  public record RefreshRequest(
      String tenantId,
      LocalDate drawDate,
      List<String> channelCodes,
      boolean force,
      boolean dryRun,
      int maxDraws) {}

  public record RefreshResponse(
      FetchExternalResultsForDateResult fetch, ApplyExternalResultsForDateResult apply) {}
}
