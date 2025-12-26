package com.tchalanet.server.core.accesscontrol.infra.web.dto;

public class PermissionResponse {
  private String code;
  private String name;
  private String category;
  private String description;

  public PermissionResponse() {}

  public PermissionResponse(String code, String name, String category, String description) {
    this.code = code;
    this.name = name;
    this.category = category;
    this.description = description;
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

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
