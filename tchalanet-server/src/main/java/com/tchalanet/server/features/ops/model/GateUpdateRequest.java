package com.tchalanet.server.features.ops.model;

/**
 * Request to update gate enable/disable.
 *
 * scope: "TENANT" or "GLOBAL"
 * tenant_id: required if scope=TENANT, null if scope=GLOBAL
 * enabled: true to enable, false to disable
 * reason: audit/log reason for change
 */
public record GateUpdateRequest(
    String scope,
    String tenant_id,
    boolean enabled,
    String reason
) {}
