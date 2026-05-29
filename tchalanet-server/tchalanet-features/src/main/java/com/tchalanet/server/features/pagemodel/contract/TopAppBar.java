package com.tchalanet.server.features.pagemodel.contract;

import java.util.List;
import java.util.Map;

/**
 * Top app bar payload for private dashboard surfaces.
 *
 * <p>Per {@code harden-pagemodel-security-v2} D5: the top app bar MUST NOT contain
 * main navigation destinations — it holds only a title, left/right action slots,
 * and a profile menu. Main navigation belongs in the {@link NavigationDrawer}.
 *
 * <p>Field {@code context} carries surface-specific metadata (e.g. current
 * tenant name) as a free-form map. It is allowed under the D4 contract because
 * it lives in a structural sub-object, not the main rendering payload.
 */
public record TopAppBar(
    TopAppBarTitle title,
    Map<String, Object> context,
    List<NavigationEntry> leftActions,
    List<NavigationEntry> actions,
    TopAppBarProfile profile) {

  /**
   * Title label resolved from an i18n key.
   */
  public record TopAppBarTitle(
      String labelKey,
      String label) {

    public static TopAppBarTitle ofKey(String labelKey) {
      return new TopAppBarTitle(labelKey, null);
    }
  }

  /**
   * Profile area — avatar + dropdown menu.
   */
  public record TopAppBarProfile(
      String displayName,
      ImageRef avatar,
      List<NavigationEntry> menu) {}
}
