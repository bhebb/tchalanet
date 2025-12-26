package com.tchalanet.server.core.draw.infra.web.ops;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.draw.infra.batch.results.fetch.DrawResultsJobStarter;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryGameRegistry;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/draw-results")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DrawResultsOpsController {

  private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ISO_LOCAL_DATE;

  private final DrawResultsJobStarter jobStarter;
  private final UsLotteryGameRegistry usLotteryGameRegistry;
  private final Clock clock;

  @PostMapping("/fetch")
  @RequiresPermission("uslottery.refresh_results")
  public ResponseEntity<FetchDrawResultsResponse> fetch(
      @CurrentContext TchRequestContext ctx,
      @RequestBody(required = false) FetchDrawResultsRequest req)
      throws Exception {

    if (req == null) {
      req = new FetchDrawResultsRequest();
    }

    if (req.channelCode() == null || req.channelCode().isBlank()) {
      throw new IllegalArgumentException("channel_code is required");
    }

    // Caps stricts
    int daysBack = Math.clamp(req.daysBack(), 0, 14);
    int maxDraws = Math.clamp(req.maxDraws(), 1, 5000);

    Map<String, String> baseParams = new HashMap<>();
    baseParams.put("ops_trigger", "true");
    baseParams.put("channel_code", req.channelCode());
    baseParams.put("max_draws", Integer.toString(maxDraws));
    baseParams.put("dry_run", Boolean.toString(req.dryRun()));
    baseParams.put("force", Boolean.toString(req.force()));

    if (ctx.effectiveTenantUuid() != null) {
      baseParams.put("tenant_id", ctx.effectiveTenantUuid().toString());
    }
    if (ctx.userUuid() != null) {
      baseParams.put("triggered_by", ctx.userUuid().toString());
    }
    if (ctx.requestId() != null) {
      baseParams.put("request_id", ctx.requestId());
    }

    // draw_date: si fourni => base, sinon today dans timezone du channel
    LocalDate baseDate = resolveBaseDate(req.channelCode(), req.drawDate());

    List<Long> executionIds = new ArrayList<>();

    for (int i = 0; i <= daysBack; i++) {
      LocalDate date = baseDate.minusDays(i);

      Map<String, String> params = new HashMap<>(baseParams);
      params.put("ts", Long.toString(System.currentTimeMillis()));
      params.put("draw_date", date.format(YYYY_MM_DD));
      params.put("days_back", "0"); // strict: la boucle est côté controller

      log.info(
          "OPS fetch_draw_results triggered: requestId={}, user={}, tenant={}, channelCode={}, drawDate={}, force={}, maxDraws={}, dryRun={}",
          ctx.requestId(),
          ctx.userUuid(),
          ctx.effectiveTenantUuid(),
          req.channelCode(),
          params.get("draw_date"),
          req.force(),
          maxDraws,
          req.dryRun());

      var execution = jobStarter.startFetchDrawResultsJob(params);
      executionIds.add(execution.getId());
    }

    String jobName = "fetch_draw_results";

    var response =
        new FetchDrawResultsResponse(jobName, true, ctx.requestId(), baseParams, -1L, executionIds);

    return ResponseEntity.accepted().body(response);
  }

  private LocalDate resolveBaseDate(String channelCode, String drawDate) {
    if (drawDate != null && !drawDate.isBlank()) {
      return LocalDate.parse(drawDate, YYYY_MM_DD);
    }

    ZoneId zone = ZoneId.of("UTC");
    var infoOpt = usLotteryGameRegistry.resolve(channelCode);
    if (infoOpt.isPresent()) {
      zone = infoOpt.get().timezone();
    }
    return ZonedDateTime.now(clock).withZoneSameInstant(zone).toLocalDate();
  }

  public record FetchDrawResultsRequest(
      String channelCode,
      @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "drawDate must be YYYY-MM-DD")
          String drawDate,
      boolean force,
      @Min(0) @Max(14) int daysBack,
      @Min(1) @Max(5000) int maxDraws,
      boolean dryRun) {

    public FetchDrawResultsRequest() {
      this(null, null, false, 0, 500, false);
    }
  }

  public record FetchDrawResultsResponse(
      String jobName,
      boolean started,
      String requestId,
      Map<String, String> params,
      long executionId,
      List<Long> executionIds) {}
}
