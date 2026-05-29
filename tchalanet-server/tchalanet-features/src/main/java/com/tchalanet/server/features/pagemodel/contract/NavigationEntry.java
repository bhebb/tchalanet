package com.tchalanet.server.features.pagemodel.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Unified navigation entry — covers both {@code "destination"} and {@code "action"} items in
 * shell fragments (top app bar, navigation drawer, profile menu).
 *
 * <p>Discriminated by {@code type}:
 * <ul>
 *   <li>{@code "destination"} — routable link; uses {@code activeMatch}, {@code badge},
 *       {@code children}.
 *   <li>{@code "action"}      — stateless action trigger; uses {@code style}, {@code confirm}.
 * </ul>
 *
 * <p>JSON field names use snake_case where the shell fragment schema requires it
 * ({@code label_key}, {@code active_match}, {@code reason_key}).
 */
public record NavigationEntry(
    String id,
    String type,
    @JsonProperty("label_key")   String labelKey,
    String label,
    String path,
    String kind,
    String icon,
    ImageRef image,

    // destination-specific
    @JsonProperty("active_match") String activeMatch,
    boolean disabled,
    @JsonProperty("reason_key")  String reasonKey,
    Badge badge,
    List<NavigationEntry> children,

    // action-specific
    String style,
    Object confirm) {

  // ---- destination factory methods -----------------------------------------

  /** Internal link destination (active match = "prefix"). */
  public static NavigationEntry destination(String id, String labelKey, String path, String icon) {
    return new NavigationEntry(id, "destination", labelKey, null, path, "internal", icon,
        null, "prefix", false, null, null, List.of(), null, null);
  }

  /** Internal link destination with exact active match. */
  public static NavigationEntry exactDestination(String id, String labelKey, String path,
      String icon) {
    return new NavigationEntry(id, "destination", labelKey, null, path, "internal", icon,
        null, "exact", false, null, null, List.of(), null, null);
  }

  // ---- action factory methods -----------------------------------------------

  /** Stateless action (no routing). */
  public static NavigationEntry action(String id, String labelKey, String icon) {
    return new NavigationEntry(id, "action", labelKey, null, null, "action", icon,
        null, null, false, null, null, null, "neutral", null);
  }

  /** Primary-styled action (e.g. CTA button). */
  public static NavigationEntry primaryAction(String id, String labelKey, String icon,
      String path) {
    return new NavigationEntry(id, "action", labelKey, null, path, "internal", icon,
        null, null, false, null, null, null, "primary", null);
  }
}
