package com.tchalanet.server.features.pagemodel.contract;

import java.util.List;

/**
 * Generic action/navigation item used by PageModel fragments and quick-action payloads.
 *
 * <p>JSON field names are camelCase. Database/seed internals may keep snake_case before mapping,
 * but emitted API contracts use {@code labelKey}, {@code activeMatch}, {@code reasonKey}, and
 * {@code requiredRoles}.
 */
public record ActionItem(
    String id,
    String kind,
    String labelKey,
    String label,
    NavigationDestination destination,
    String icon,
    String image,
    String activeMatch,
    boolean disabled,
    String reasonKey,
    Object badge,
    List<ActionItem> children) {

  /** Backward-compatible constructor for existing dashboard quick-action assemblers. */
  public ActionItem(String id, String labelKey, String icon, String path) {
    this(
        id,
        "link",
        labelKey,
        null,
        NavigationDestination.route(path),
        icon,
        null,
        null,
        false,
        null,
        null,
        List.of());
  }
}
