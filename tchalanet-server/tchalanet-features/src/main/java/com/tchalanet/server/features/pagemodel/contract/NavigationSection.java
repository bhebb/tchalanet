package com.tchalanet.server.features.pagemodel.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A labelled section grouping navigation destinations inside a navigation drawer.
 *
 * <p>Uses {@code items} (matching the shell fragment JSON schema) rather than
 * {@code destinations} to stay consistent with what the frontend receives from
 * JSON file fragments.
 */
public record NavigationSection(
    String id,
    @JsonProperty("label_key") String labelKey,
    String label,
    List<NavigationEntry> items) {

  public static NavigationSection of(String id, String labelKey,
      List<NavigationEntry> items) {
    return new NavigationSection(id, labelKey, null, items);
  }
}
