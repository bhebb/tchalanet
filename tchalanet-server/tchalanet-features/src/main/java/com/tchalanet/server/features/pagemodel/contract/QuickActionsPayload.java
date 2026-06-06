package com.tchalanet.server.features.pagemodel.contract;

import java.util.List;

/**
 * Typed contract for a QuickActionsWidget payload.
 * Uses {@code ActionItem} records (typed {@code labelKey} + {@code destination}).
 */
public record QuickActionsPayload(List<ActionItem> actions) {

  public static QuickActionsPayload empty() {
    return new QuickActionsPayload(List.of());
  }
}
