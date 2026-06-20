package com.tchalanet.server.platform.accesscontrol.api.model.view;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AccessSnapshotView(
    UserId userId,
    PlatformAccessView platform,
    List<TenantAccessScopeView> tenantScopes,
    SellerTerminalAccessScopeView sellerTerminalScope) {

  public record PlatformAccessView(
      boolean superAdmin,
      Set<String> roleCodes,
      Set<String> permissionKeys) {}

  public record TenantAccessScopeView(
      TenantId tenantId,
      String tenantCode,
      String tenantName,
      String tenantStatus,
      Set<String> roleCodes,
      Set<String> permissionKeys) {}

  public record SellerTerminalAccessScopeView(
      UUID sellerTerminalId,
      TenantId tenantId,
      String tenantCode,
      String terminalCode,
      String status,
      Set<String> permissionKeys) {}
}
