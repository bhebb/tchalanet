package com.tchalanet.server.features.tenantadmin.draw;

import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.DrawResultSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminDrawOverviewResponse(
    DrawInfo draw,
    List<TopSelectionItem> topSelections,
    List<EffectiveLimitItem> effectiveLimits,
    List<ExposureAlertItem> exposureAlerts) {

  public record DrawInfo(
      UUID drawId,
      String channelCode,
      String channelLabel,
      LocalDate drawDate,
      DrawStatus status,
      Instant scheduledAt,
      Instant openedAt,
      Instant closedAt,
      DrawResultSummary result) {}

  public record TopSelectionItem(
      int rank,
      String displaySelection,
      String gameCode,
      String betType,
      int count,
      long totalStakeCents) {}

  public record EffectiveLimitItem(String ruleKey, String resolvedScope) {}

  /** Numéro à risque : sélection dont l'exposition approche du plafond configuré. */
  public record ExposureAlertItem(
      String betType,
      String selectionKey,
      BigDecimal stakeTotal,
      long salesCount,
      BigDecimal maxStakeExposureLimit,
      BigDecimal stakeRatio) {}
}
