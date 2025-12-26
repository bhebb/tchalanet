package com.tchalanet.server.features.private_dashboard.block;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import java.time.Instant;
import java.util.List;

public record ValidationsBlock(List<ValidationItem> items) {
  public static ValidationsBlock empty() {
    return new ValidationsBlock(List.of());
  }

  public record ValidationItem(
      String id,
      String labelKey,
      String target,
      String amount,
      String requestedBy,
      Instant requestedAt,
      // autonomy fields
      AutonomyLevel level,
      boolean requireApprovalOnBlock,
      ApprovalRole approvalRole,
      boolean enabled,
      Instant startsAt,
      Instant endsAt) {}
}
