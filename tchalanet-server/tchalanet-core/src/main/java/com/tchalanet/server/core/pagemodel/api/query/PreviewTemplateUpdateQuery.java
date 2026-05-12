package com.tchalanet.server.core.pagemodel.api.query;

import com.tchalanet.server.common.bus.Query;
import jakarta.validation.constraints.NotBlank;

public record PreviewTemplateUpdateQuery(@NotBlank String logicalId)
    implements Query<TemplateUpdatePreviewView> {}
