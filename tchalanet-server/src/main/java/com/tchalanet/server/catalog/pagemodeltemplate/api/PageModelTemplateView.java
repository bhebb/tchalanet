package com.tchalanet.server.catalog.pagemodeltemplate.api;

import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

/**
 * Public view for PageModelTemplate (admin view)
 */
public record PageModelTemplateView(
    PageModelTemplateId id,
    String code,
    TenantId tenantId,
    String logicalId,
    String name,
    String label,
    String description,
    int schemaVersion,
    String modelJson,
    boolean isDefault,
    boolean isSystem) {}
