package com.tchalanet.server.platform.tenantconfig.api.model;


/**
 * Query: Get tenant by code.
 * Returns full TenantConfigView instead of just TenantId.
 * Per user request: resolveByCode should return TenantConfigView.
 */
public record GetTenantByCodeQuery(String code) {}
