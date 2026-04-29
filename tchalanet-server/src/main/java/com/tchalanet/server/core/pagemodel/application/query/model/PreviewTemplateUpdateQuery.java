package com.tchalanet.server.core.pagemodel.application.query.model;

import com.tchalanet.server.common.bus.Query;
import jakarta.validation.constraints.NotBlank;

public record PreviewTemplateUpdateQuery(@NotBlank String logicalId)
    implements Query<TemplateUpdatePreviewView> {}
