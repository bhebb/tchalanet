package com.tchalanet.server.features.tenantadmin.infra.web.model;

public record TenantConfigRequest(String timeZone, String currency, String locale) {}
