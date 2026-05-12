package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrantStatus;

public record IssueOfflineSalesGrantResult(OfflineSalesGrantId grantId, OfflineSalesGrantStatus status) {}

