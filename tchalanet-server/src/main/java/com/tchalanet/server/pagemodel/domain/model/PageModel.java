package com.tchalanet.server.pagemodel.domain.model;

import java.util.UUID;

public class PageModel {

  private final UUID id;
  private final UUID tenantId; // peut être null pour tchalanet global
  private final String code; // ex: "public_home"
  private final String lang; // "fr", "en", "ht"
  private final String json; // JSON complet du PageModel

  public PageModel(UUID id, UUID tenantId, String code, String lang, String json) {
    this.id = id;
    this.tenantId = tenantId;
    this.code = code;
    this.lang = lang;
    this.json = json;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getCode() {
    return code;
  }

  public String getLang() {
    return lang;
  }

  public String getJson() {
    return json;
  }
}
