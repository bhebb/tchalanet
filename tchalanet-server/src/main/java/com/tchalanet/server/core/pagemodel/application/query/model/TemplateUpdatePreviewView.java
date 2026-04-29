package com.tchalanet.server.core.pagemodel.application.query.model;

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
