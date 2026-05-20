package com.tchalanet.server.core.pagemodel.api.query;

import com.tchalanet.server.common.types.id.PageModelTemplateId;

public record TemplateUpdatePreviewView(
    PageModelTemplateId templateId,
    String logicalId,
    int currentSchemaVersion,
    int templateSchemaVersion,
    boolean stale,
    TemplateUpdateCompatibility compatibility,
    TemplateUpdateRecommendedAction recommendedAction,
    TemplateUpdateDiffView diff) {}
