package com.tchalanet.server.features.pagemodel.contract;

import java.util.List;

/**
 * Navigation drawer payload for private dashboard surfaces (cashier, tenant admin, superadmin).
 *
 * <p>Structure:
 * <ul>
 *   <li>{@code brand}               — logo + surface title at the top of the drawer
 *   <li>{@code topDestinations}     — primary shortcut links (ungrouped, shown first)
 *   <li>{@code sections}            — labelled groups of destinations (shown below top)
 *   <li>{@code footerDestinations}  — utility links at the bottom (support, logout, etc.)
 *   <li>{@code actions}             — action entries (rare; usually empty)
 * </ul>
 */
public record NavigationDrawer(
    BrandBlock brand,
    List<NavigationEntry> topDestinations,
    List<NavigationSection> sections,
    List<NavigationEntry> footerDestinations,
    List<NavigationEntry> actions) {

  public static NavigationDrawer of(
      BrandBlock brand,
      List<NavigationEntry> topDestinations,
      List<NavigationSection> sections,
      List<NavigationEntry> footerDestinations) {
    return new NavigationDrawer(brand, topDestinations, sections, footerDestinations, List.of());
  }
}
