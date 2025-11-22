package com.tchalanet.server.common.usecase;

import com.tchalanet.server.common.web.dto.ContextDto;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Use case pour récupérer le contexte d'un tenant. Version mock pour le moment - à implémenter avec
 * une vraie logique.
 */
@Service
public class GetTenantContextUseCase {

  private static final Map<String, ContextDto> MOCKS =
      Map.of(
          key("default", "core"),
          new ContextDto(
              List.of("dashboard", "tickets", "results", "reports"),
              Map.of("menu.dashboard", "Tableau de bord", "menu.tickets", "Tickets / Jouer"),
              Map.of("primary", "#0057B8", "accent", "#FFD700", "mode", "light")),
          key("vip", "core"),
          new ContextDto(
              List.of("dashboard", "tickets", "results", "reports", "vip-support"),
              Map.of("menu.dashboard", "Espace VIP"),
              Map.of("primary", "#7c3aed", "accent", "#22d3ee", "mode", "dark")));

  private static String key(String tenantId, String featureSetId) {
    return tenantId + "|" + featureSetId;
  }

  public ContextDto execute(String tenantId, String featureSetId) {
    return MOCKS.getOrDefault(
        key(tenantId, featureSetId),
        new ContextDto(
            List.of("dashboard"), Map.of(), Map.of("primary", "#1976d2", "mode", "light")));
  }
}
