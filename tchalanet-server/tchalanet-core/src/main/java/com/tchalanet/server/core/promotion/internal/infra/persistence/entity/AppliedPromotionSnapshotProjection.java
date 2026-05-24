package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AppliedPromotionSnapshotProjection(
    UUID ticketId,
    UUID promotionDecisionId,
    String decisionStatus,
    Instant appliedAt,
    Map<String, Object> snapshotJson
) {}

