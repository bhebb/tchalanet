package com.tchalanet.server.common.web.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Données dynamiques pour la page d'accueil publique: plans, jeux, tirages, prochain tirage, etc.
 */
public record PublicHomeData(
    UUID tenantId,
    List<Map<String, Object>> plans,
    List<Map<String, Object>> games,
    List<Map<String, Object>> drawsToday,
    Map<String, Object> nextDraw,
    List<Map<String, Object>> news) {}
