package com.tchalanet.server.core.tenant.domain.model;

/** Modèle métier de base d'un tenant. */
public class Tenant {

  private final TenantId id;
  private final String name;

  // TODO: ajouter thème actif, plan courant, limites, statut, etc.

  public Tenant(TenantId id, String name) {
    if (id == null) throw new IllegalArgumentException("id is required");
    if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
    this.id = id;
    this.name = name;
  }

  public TenantId id() {
    return id;
  }

  public String name() {
    return name;
  }
}
