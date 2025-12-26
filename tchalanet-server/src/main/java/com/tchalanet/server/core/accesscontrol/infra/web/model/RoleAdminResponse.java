package com.tchalanet.server.core.accesscontrol.infra.web.dto;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public class RoleAdminResponse {

  private UUID id;
  private String code;
  private String name;
  private String description;
  private UUID tenantId;
  private UUID parentRoleId;
  private boolean system;

  public RoleAdminResponse() {}

  public RoleAdminResponse(
      UUID id,
      String code,
      String name,
      String description,
      UUID tenantId,
      UUID parentRoleId,
      boolean system) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.description = description;
    this.tenantId = tenantId;
    this.parentRoleId = parentRoleId;
    this.system = system;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getParentRoleId() {
    return parentRoleId;
  }

  public void setParentRoleId(UUID parentRoleId) {
    this.parentRoleId = parentRoleId;
  }

  public boolean isSystem() {
    return system;
  }

  public void setSystem(boolean system) {
    this.system = system;
  }
}
