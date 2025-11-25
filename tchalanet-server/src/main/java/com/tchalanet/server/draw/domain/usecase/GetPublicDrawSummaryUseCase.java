package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.draw.domain.dto.PublicDrawSummary;
import java.util.UUID;

public interface GetPublicDrawSummaryUseCase {
  PublicDrawSummary getSummaryForTenant(UUID tenantId);
}
