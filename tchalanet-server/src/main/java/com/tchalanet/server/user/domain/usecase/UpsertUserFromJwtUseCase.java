package com.tchalanet.server.user.domain.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Use case pour synchroniser un utilisateur depuis le JWT lors de l'authentification. Crée ou met à
 * jour l'utilisateur dans la table app_user.
 */
@Service
@RequiredArgsConstructor
public class UpsertUserFromJwtUseCase {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Synchronise l'utilisateur depuis le JWT vers la base de données. Effectue un UPSERT (INSERT ou
   * UPDATE si existe déjà).
   */
  public void execute(Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    String username = jwt.getClaimAsString("preferred_username");
    String email = jwt.getClaimAsString("email");
    String activeEnterpriseId = jwt.getClaimAsString("active_enterprise_id");

    // Extraction et sérialisation du claim "tenants"
    String tenantJson = extractTenantJson(jwt);

    jdbcTemplate.update(
        """
        INSERT INTO app_user (id, username, email, tenant, active_enterprise_id, last_login_at)
        VALUES (?, ?, ?, ?, ?, now())
        ON CONFLICT (id) DO UPDATE
          SET username = EXCLUDED.username,
              email = EXCLUDED.email,
              tenant = EXCLUDED.tenant,
              active_enterprise_id = EXCLUDED.active_enterprise_id,
              last_login_at = now()
        """,
        userId,
        username,
        email,
        tenantJson,
        activeEnterpriseId);
  }

  /** Extrait et sérialise le claim "tenants" du JWT en JSON. */
  private String extractTenantJson(Jwt jwt) {
    Object tenantInToken = jwt.getClaim("tenants");

    // Si c'est déjà une Map ou Collection, sérialiser en JSON
    if (tenantInToken instanceof Map<?, ?> || tenantInToken instanceof Collection) {
      try {
        return objectMapper.writeValueAsString(tenantInToken);
      } catch (JsonProcessingException e) {
        // En cas d'erreur, retourner une valeur par défaut
        return "tenant-dev";
      }
    }

    // Valeur par défaut
    return "tenant-dev";
  }
}
