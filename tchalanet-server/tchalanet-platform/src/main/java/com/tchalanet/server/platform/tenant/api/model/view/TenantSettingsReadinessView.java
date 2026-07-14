package com.tchalanet.server.platform.tenant.api.model.view;

import java.util.List;

public record TenantSettingsReadinessView(boolean ready, List<SectionReadiness> sections) {
  public TenantSettingsReadinessView {
    sections = sections == null ? List.of() : List.copyOf(sections);
  }

  public record SectionReadiness(
      String section, ReadinessStatus status, List<String> blockingReasons, List<String> warnings) {
    public SectionReadiness {
      blockingReasons = blockingReasons == null ? List.of() : List.copyOf(blockingReasons);
      warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
  }

  public enum ReadinessStatus {
    READY,
    ACTION_REQUIRED
  }
}
