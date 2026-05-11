package com.tchalanet.server.core.offlinesync.application.command.model.grant;

import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSalesGrantStatus;

public record IssueOfflineSalesGrantResult(OfflineSalesGrantId grantId, OfflineSalesGrantStatus status) {}

