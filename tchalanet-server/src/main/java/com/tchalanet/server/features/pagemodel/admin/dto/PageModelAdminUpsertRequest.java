package com.tchalanet.server.features.pagemodel.admin.dto;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import java.util.UUID;

public record PageModelAdminUpsertRequest(
    UUID id, String logicalId, String scope, String slug, Integer schemaVersion, PageModel model) {}
