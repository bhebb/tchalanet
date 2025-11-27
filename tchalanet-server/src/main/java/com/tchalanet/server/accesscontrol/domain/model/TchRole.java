package com.tchalanet.server.accesscontrol.domain.model;

/**
 * Rôles "système" Tchalanet, mappés aux rôles globaux (is_system=true dans app_role). Utilisé
 * surtout pour le mapping avec Keycloak / configuration.
 */
public enum TchRole {
  SUPER_ADMIN, // plateforme entière
  TENANT_ADMIN, // admin d'un tenant
  OPERATOR, // chef de PDV / opérateur
  CASHIER // caissier POS
}
