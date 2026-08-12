package com.tchalanet.server.features.pos.draws;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PosDrawDetailResponse(
    UUID drawId,
    List<TopSelectionItem> topSelections,
    ExposureInfo exposure) {

  public record TopSelectionItem(
      String selectionKey,
      String betType,
      long stakeTotalCents,
      long salesCount) {}

  /**
   * Exposure information for this draw scoped to the current seller terminal.
   *
   * <p>{@code active = true} when the draw is OPEN and exposure data is available.
   * {@code active = false} when the draw is CLOSED — top selections remain visible in the
   * report, but exposure/action context is inactive.
   *
   * <p>{@code limitConfigured = true} when a MAX_STAKE_EXPOSURE rule is configured for this draw.
   */
  public record ExposureInfo(
      boolean active,
      boolean limitConfigured,
      List<ExposureAlertItem> alerts) {}

  public record ExposureAlertItem(
      String betType,
      String selectionKey,
      BigDecimal stakeTotal,
      long salesCount,
      BigDecimal maxStakeExposureLimit,
      BigDecimal ratio) {}
}
