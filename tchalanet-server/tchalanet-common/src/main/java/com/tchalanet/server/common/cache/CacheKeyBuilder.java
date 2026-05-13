package com.tchalanet.server.common.cache;

import com.tchalanet.server.common.types.id.TenantId;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

  public String appSettingsKey(UUID tenantId, UUID outletId) {
    return "tch:%s:%s:settings:app:outlet:%s".formatted(env, tenantId, outletId);
  }

  public String tenantTerminalKey(UUID tenantId, UUID terminalId) {
    return "tch:%s:%s:terminal:%s".formatted(env, tenantId, terminalId);
  }

  public String tenantOutletTreeKey(TenantId tenantId) {
    return "tch:%s:%s:outlet:tree".formatted(env, tenantId.value());
  }

  public String tenantDrawsSummaryKey(TenantId tenantId) {
    return "tch:%s:%s:draws:summary".formatted(env, tenantId.value());
  }

  public String tenantDrawsChannelKey(TenantId tenantId, String channelCode, String kind) {
    return "tch:%s:%s:draws:%s:%s".formatted(env, tenantId.value(), channelCode, kind);
  }

  public String tenantDrawPublicKey(TenantId tenantId, String date) {
    return "tch:%s:%s:draws:public:%s".formatted(env, tenantId.value(), date);
  }

  public String userPermissionsKey(TenantId tenantId, UUID userId) {
    return "tch:%s:%s:user:%s:permissions".formatted(env, tenantId.value(), userId);
  }

  public String userProfileKey(UUID tenantId, UUID userId) {
    return "tch:%s:%s:user:%s:profile".formatted(env, tenantId, userId);
  }

  public String tenantRolesMatrixKey(UUID tenantId) {
    return "tch:%s:%s:roles:matrix".formatted(env, tenantId);
  }

  public String usLotteryProviderRawKey(
      String provider, LocalDate drawDate, @Nullable String queryHash) {
    if (provider == null || provider.isBlank()) throw new IllegalArgumentException("provider");
    if (drawDate == null) throw new IllegalArgumentException("drawDate");

    var key =
        "tch:%s:uslottery:raw:v1:%s:%s".formatted(env, provider.trim().toUpperCase(), drawDate);

    if (queryHash != null && !queryHash.isBlank()) {
      key += ":" + queryHash;
    }
    return key;
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
