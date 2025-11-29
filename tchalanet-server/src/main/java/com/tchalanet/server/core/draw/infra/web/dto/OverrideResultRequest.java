package com.tchalanet.server.core.draw.infra.web.dto;

import java.util.List;
import java.util.Map;

/** Payload attendu pour forcer un résultat manuel sur un tirage. */
public record OverrideResultRequest(List<String> numbers, Map<String, Object> extra) {}
