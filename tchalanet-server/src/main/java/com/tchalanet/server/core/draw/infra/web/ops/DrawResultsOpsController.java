package com.tchalanet.server.core.draw.infra.web.ops;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.core.draw.infra.batch.results.fetch.DrawResultsJobStarter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/draw-results")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Ops • Scheduler")
public class DrawResultsOpsController {

  private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ISO_LOCAL_DATE;

  private final DrawResultsJobStarter jobStarter;
  private final Clock clock;

  private final TchContextResolver contextResolver;

  @Operation(summary = "Trigger fetch of draw results (ops)")
  @PostMapping("/fetch")
  //    @RequiresPermission("uslottery.refresh_results") //todo revert
  public ResponseEntity<FetchDrawResultsResponse> fetch(
      @RequestBody(required = false) FetchDrawResultsRequest req) throws Exception {

    var ctx = contextResolver.currentOrNull();
    if (req == null) {
      req = new FetchDrawResultsRequest(List.of(), null, false, 0, 500, false);
    }

    if (req.channelCodes() == null || req.channelCodes().isEmpty()) {
      throw new IllegalArgumentException("channelCodes is required");
    }

    // Caps stricts
    int daysBackVar = Math.clamp(req.daysBack(), 0, 14);
    int maxDrawsVar = Math.clamp(req.maxDraws(), 1, 5000);

    // normalize + dedupe
    final List<String> channelCodesNormalized =
        req.channelCodes().stream()
            .filter(Objects::nonNull)
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .filter(s -> !s.isBlank())
            .distinct()
            .toList();

    if (channelCodesNormalized.isEmpty())
      throw new IllegalArgumentException("channelCodes is required");
    if (channelCodesNormalized.size() > 80)
      throw new IllegalArgumentException("channelCodes too large (max 80)");

    final boolean forceFlag = req.force();
    final boolean dryRunFlag = req.dryRun();

    var baseParams = new HashMap<String, String>();
    baseParams.put("ops_trigger", "true");
    baseParams.put("max_draws", Integer.toString(maxDrawsVar));
    baseParams.put("dry_run", Boolean.toString(dryRunFlag));
    baseParams.put("force", Boolean.toString(forceFlag));

    // pass list once as CSV + hash
    String csv = String.join(",", channelCodesNormalized);
    baseParams.put("channel_codes", csv);
    baseParams.put("channel_codes_hash", sha256Hex(csv));

    if (ctx != null && ctx.effectiveTenantUuid() != null) {
      baseParams.put("tenant_id", ctx.effectiveTenantUuid().toString());
    }
    if (ctx != null && ctx.userUuid() != null) {
      baseParams.put("triggered_by", ctx.userUuid().toString());
    }
    if (ctx != null && ctx.requestId() != null) {
      baseParams.put("request_id", ctx.requestId());
    }

    // baseDate: if drawDate not provided -> today UTC
    final LocalDate baseDateVar =
        (req.drawDate() != null && !req.drawDate().isBlank())
            ? LocalDate.parse(req.drawDate(), YYYY_MM_DD)
            : ZonedDateTime.now(clock).toLocalDate();

    var executionIds =
        IntStream.rangeClosed(0, daysBackVar)
            .mapToObj(
                i -> {
                  var date = baseDateVar.minusDays(i);
                  var params = new HashMap<>(baseParams);
                  params.put("ts", Long.toString(System.currentTimeMillis()));
                  params.put("draw_date", date.format(YYYY_MM_DD));
                  params.put("days_back", "0"); // controller loops by days

                  if (ctx != null) {
                    log.info(
                        "OPS fetch_draw_results triggered: requestId={}, user={}, tenant={}, channelCount={}, drawDate={}, force={}, maxDraws={}, dryRun= {}",
                        ctx.requestId(),
                        ctx.userUuid(),
                        ctx.effectiveTenantUuid(),
                        channelCodesNormalized.size(),
                        params.get("draw_date"),
                        forceFlag,
                        maxDrawsVar,
                        dryRunFlag);
                  } else {
                    log.info(
                        "OPS fetch_draw_results triggered for anonymous context: channelCount={}, drawDate={}",
                        channelCodesNormalized.size(),
                        date);
                  }

                  try {
                    var execution = jobStarter.startFetchDrawResultsJob(params);
                    return execution == null ? null : execution.getId();
                  } catch (Exception ex) {
                    log.warn("Failed to start job for date {}: {}", date, ex.getMessage());
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .toList();

    var jobName = "fetch_draw_results";

    var response =
        new FetchDrawResultsResponse(
            jobName, true, ctx == null ? null : ctx.requestId(), baseParams, -1L, executionIds);

    return ResponseEntity.accepted().body(response);
  }

  private static String sha256Hex(String s) {
    try {
      var md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      var sb = new StringBuilder();
      for (byte b : dig) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException("sha256 failed", e);
    }
  }

  public record FetchDrawResultsRequest(
      List<String> channelCodes,
      @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "drawDate must be YYYY-MM-DD")
          String drawDate,
      boolean force,
      @Min(0) @Max(14) int daysBack,
      @Min(1) @Max(5000) int maxDraws,
      boolean dryRun) {}

  public record FetchDrawResultsResponse(
      String jobName,
      boolean started,
      String requestId,
      Map<String, String> params,
      long executionId,
      List<Long> executionIds) {}
}
