package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record LimitAssignment(
    LimitAssignmentId id,
    LimitDefinitionId limitDefinitionId,
    LimitTarget target,
    boolean enabled,
    Instant startsAt,
    Instant endsAt,
    JsonNode paramsOverride,
    JsonNode appliesToOverride,
    Instant deletedAt) {

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public boolean isActiveAt(Instant now) {
    if (deletedAt != null) return false;
    if (!enabled) return false;
    if (startsAt != null && now.isBefore(startsAt)) return false;
    if (endsAt != null && !now.isBefore(endsAt)) return false;
    return true;
  }

  /** Single canonical predicate used by LimitResolver. */
  public boolean appliesTo(LimitTarget candidateTarget, Instant now) {
    if (!isActiveAt(now)) return false;
    return target != null && target.equals(candidateTarget);
  }
}
