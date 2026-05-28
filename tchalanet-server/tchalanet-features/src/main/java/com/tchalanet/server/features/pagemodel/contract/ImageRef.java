package com.tchalanet.server.features.pagemodel.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Image reference — used in brand blocks, navigation entries and profile avatars.
 *
 * <p>JSON field names use snake_case ({@code alt_key}, {@code title_key},
 * {@code aspect_ratio}) to match the shell fragment JSON schema.
 */
public record ImageRef(
    String url,
    @JsonProperty("alt_key")     String altKey,
    @JsonProperty("title_key")   String titleKey,
    Integer width,
    Integer height,
    @JsonProperty("aspect_ratio") String aspectRatio,
    String loading) {

  /** Standard logo/avatar preset (36×36, eager). */
  public static ImageRef logo(String url, String altKey) {
    return new ImageRef(url, altKey, null, 36, 36, "1:1", "eager");
  }

  /** Standard avatar preset (32×32, eager). */
  public static ImageRef avatar(String url, String altKey) {
    return new ImageRef(url, altKey, null, 32, 32, "1:1", "eager");
  }
}
