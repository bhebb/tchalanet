package com.tchalanet.server.features.pagemodel.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Brand block displayed at the top of a navigation drawer or public header.
 *
 * <p>JSON field names match the shell fragment schema (snake_case for
 * {@code label_key}, {@code subtitle_key}).
 */
public record BrandBlock(
    String id,
    @JsonProperty("label_key")    String labelKey,
    String label,
    @JsonProperty("subtitle_key") String subtitleKey,
    String path,
    ImageRef image) {

  /** Minimal brand block without subtitle (public header). */
  public static BrandBlock of(String id, String labelKey, String path, ImageRef image) {
    return new BrandBlock(id, labelKey, null, null, path, image);
  }

  /** Brand block with a surface subtitle (private navigation drawer). */
  public static BrandBlock withSubtitle(String id, String labelKey, String subtitleKey,
      String path, ImageRef image) {
    return new BrandBlock(id, labelKey, null, subtitleKey, path, image);
  }
}
