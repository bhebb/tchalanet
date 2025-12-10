package com.tchalanet.server.core.tenant.domain.model;

/** Modèle métier de base d'un tenant. */
public class Tenant {

  private final TenantId id;
  private final String name;
  private final TenantType type;

  // TODO: ajouter thème actif, plan courant, limites, statut, etc.

  public Tenant(TenantId id, String name, TenantType type) {
    if (id == null) throw new IllegalArgumentException("id is required");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    if (type == null) throw new IllegalArgumentException("type is required");
    this.id = id;
    this.name = name;
    this.type = type;
  }

  public TenantId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public TenantType type() {
    return type;
  }
}
