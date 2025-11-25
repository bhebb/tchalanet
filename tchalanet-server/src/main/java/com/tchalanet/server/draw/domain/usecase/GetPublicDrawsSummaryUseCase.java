package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.draw.domain.model.DrawSummary;

public interface GetPublicDrawsSummaryUseCase {
  DrawSummary getSummaryForTenant(java.util.UUID tenantId);
}
