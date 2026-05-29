package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import jakarta.validation.constraints.Size;

public record UpdatePromotionRuleRequest(
    @Size(max = 96)
    String ruleKey,

    Integer priority
) {}
