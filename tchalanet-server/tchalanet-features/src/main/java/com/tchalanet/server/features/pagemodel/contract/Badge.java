package com.tchalanet.server.features.pagemodel.contract;

/**
 * Notification badge displayed on a navigation entry.
 *
 * <p>{@code variant} maps to a UI theme variant: {@code "primary"}, {@code "warn"},
 * {@code "danger"}, or {@code "neutral"}.
 */
public record Badge(int count, String variant) {

  public static Badge warn(int count) {
    return new Badge(count, "warn");
  }

  public static Badge danger(int count) {
    return new Badge(count, "danger");
  }
}
