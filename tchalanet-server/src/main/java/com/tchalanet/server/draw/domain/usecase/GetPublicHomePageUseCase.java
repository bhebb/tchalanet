package com.tchalanet.server.draw.domain.usecase;

import com.tchalanet.server.draw.domain.model.DrawSummary;
import com.tchalanet.server.tenant.domain.model.Plan;
import java.util.List;
import java.util.UUID;

public interface GetPublicHomePageUseCase {
  /** Returns the public home page model for a tenant. */
  PublicHomePage getHome(UUID tenantId);

  record PublicHomePage(DrawSummary draws, List<Plan> plans) {}
}
