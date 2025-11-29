package com.tchalanet.server.features.pagemodel.application.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO representing the dynamic data to be injected into the public home page model. */
public record PublicHomeDynamicData(
    UUID tenantId,
    List<Map<String, Object>> plans,
    List<Map<String, Object>> games,
    List<Map<String, Object>> drawsToday,
    Map<String, Object> nextDraw,
    List<Map<String, Object>> news) {}
