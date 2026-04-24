package com.tchalanet.server.features.pagemodel_backup.shared.block;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeaturesBlock(List<FeatureItem> items) {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record FeatureItem(String icon, String title, String description) {}
}
