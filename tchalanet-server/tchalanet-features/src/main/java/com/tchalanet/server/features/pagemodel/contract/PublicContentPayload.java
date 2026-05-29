package com.tchalanet.server.features.pagemodel.contract;

import java.util.List;

/**
 * Typed contract for a public-content widget payload (news/announcements).
 * Shared between tenant admin, platform admin and cashier public-content widgets.
 */
public record PublicContentPayload(
    List<NewsItem> items,
    int count) {

  public static PublicContentPayload empty() {
    return new PublicContentPayload(List.of(), 0);
  }
}
