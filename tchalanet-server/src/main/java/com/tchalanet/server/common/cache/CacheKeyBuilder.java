package com.tchalanet.server.common.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class CacheKeyBuilder {
    private final String env;

    public CacheKeyBuilder(@Value("${tch.env:${spring.profiles.active:dev}}") String env) {
        this.env = env == null ? "dev" : env;
    }

    public String tenantConfigKey(UUID tenantId) {
        return "tch:%s:%s:tenant:config".formatted(env, tenantId);
    }

    public String tenantThemeKey(UUID tenantId) {
        return "tch:%s:%s:theme:active".formatted(env, tenantId);
    }

    public String tenantLimitsKey(UUID tenantId) {
        return "tch:%s:%s:tenant:limits".formatted(env, tenantId);
    }

    public String tenantOutletKey(UUID tenantId, UUID outletId) {
        return "tch:%s:%s:outlet:%s".formatted(env, tenantId, outletId);
    }

    public String tenantTerminalKey(UUID tenantId, UUID terminalId) {
        return "tch:%s:%s:terminal:%s".formatted(env, tenantId, terminalId);
    }

    public String tenantOutletTreeKey(UUID tenantId) {
        return "tch:%s:%s:outlet:tree".formatted(env, tenantId);
    }

    public String tenantDrawsSummaryKey(UUID tenantId) {
        return "tch:%s:%s:draws:summary".formatted(env, tenantId);
    }

    public String tenantDrawsChannelKey(UUID tenantId, String channelCode, String kind) {
        return "tch:%s:%s:draws:%s:%s".formatted(env, tenantId, channelCode, kind);
    }

    public String tenantDrawPublicKey(UUID tenantId, String date) {
        return "tch:%s:%s:draws:public:%s".formatted(env, tenantId, date);
    }

    public String userPermissionsKey(UUID tenantId, UUID userId) {
        return "tch:%s:%s:user:%s:permissions".formatted(env, tenantId, userId);
    }

    public String userProfileKey(UUID tenantId, UUID userId) {
        return "tch:%s:%s:user:%s:profile".formatted(env, tenantId, userId);
    }

    public String tenantRolesMatrixKey(UUID tenantId) {
        return "tch:%s:%s:roles:matrix".formatted(env, tenantId);
    }

    public String globalSearchKey(String queryHash) {
        return "tch:%s:-:search:query:%s".formatted(env, queryHash);
    }

    public String newsExternalKey() {
        return "tch:public-%s:-:news".formatted(env);
    }

    public String newsInternalKey() {
        return "tch:public-%s:-:news:internal".formatted(env);
    }

    public String newsHiddenKey() {
        return "tch:public-%s:-:news:hidden".formatted(env);
    }
}
