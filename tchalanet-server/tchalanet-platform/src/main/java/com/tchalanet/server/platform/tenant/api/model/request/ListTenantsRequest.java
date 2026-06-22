package com.tchalanet.server.platform.tenant.api.model.request;

import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import org.springframework.data.domain.Pageable;

/**
 * Query: List tenants with pagination, free-text search and status filter.
 * search.likePattern() matches against code + name (case-insensitive).
 */
public record ListTenantsRequest(
    Pageable pageable,
    TchSearchQuery search,
    TenantStatus status
) {}
