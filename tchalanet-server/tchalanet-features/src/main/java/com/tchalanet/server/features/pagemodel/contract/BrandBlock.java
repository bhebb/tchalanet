package com.tchalanet.server.features.pagemodel.contract;

/**
 * Brand block displayed at the top of a navigation drawer or public header.
 *
 * <p>JSON field names are camelCase in API payloads.
 */
public record BrandBlock(
    String id,
    String labelKey,
    String label,
    String subtitleKey,
    NavigationDestination destination,
    ImageRef image) {

  /** Minimal brand block without subtitle (public header). */
  public static BrandBlock of(String id, String labelKey, String path, ImageRef image) {
    return new BrandBlock(id, labelKey, null, null, NavigationDestination.route(path), image);
  }

  /** Brand block with a surface subtitle (private navigation drawer). */
  public static BrandBlock withSubtitle(String id, String labelKey, String subtitleKey,
      String path, ImageRef image) {
    return new BrandBlock(id, labelKey, null, subtitleKey, NavigationDestination.route(path), image);
  }
}
