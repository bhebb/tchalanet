package com.tchalanet.server.pagemodel.domain.ports.in;

import com.tchalanet.server.pagemodel.domain.model.PageModel;
import java.util.UUID;

/**
 * Inbound Port for retrieving any PageModel by its code, tenant, and language. Used for private
 * pages/dashboards.
 */
public interface GetPageModelUseCase {
  PageModel getPageModel(UUID tenantId, String code, String lang);
}
