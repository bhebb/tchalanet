package com.tchalanet.server.common.security;

/**
 * Composants de sécurité globaux à l'application.
 *
 * <p>Inclut notamment :
 *
 * <ul>
 *   <li>Filtres de sécurité HTTP communs (ex: contexte user, RLS Postgres, logs d'audit globaux).
 *   <li>Configuration SecurityFilterChain partagée entre domaines.
 *   <li>Éventuelles utilitaires de mapping des rôles Keycloak → rôles applicatifs.
 * </ul>
 *
 * Les aspects de sécurité qui ne concernent qu'un seul domaine peuvent vivre dans {@code
 * <domaine>.infra.security} si nécessaire.
 */
