package com.tchalanet.server.core.analytics.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScope;
import com.tchalanet.server.core.analytics.api.model.AnalyticsVisibilityOverrideView;
import java.util.UUID;
import org.springframework.util.StringUtils;

/** Enables or disables financial metric visibility for one exact analytics scope. */
public record SetAnalyticsVisibilityOverrideCommand(
    AnalyticsTrustScope scope, boolean visible, String reason, UUID changedBy)
    implements Command<AnalyticsVisibilityOverrideView> {

  public SetAnalyticsVisibilityOverrideCommand {
    if (scope == null) throw new IllegalArgumentException("scope must not be null");
    if (!StringUtils.hasText(reason) || reason.trim().length() < 10) {
      throw new IllegalArgumentException("reason must contain at least 10 characters");
    }
    if (changedBy == null) throw new IllegalArgumentException("changedBy must not be null");
    reason = reason.trim();
  }
}
