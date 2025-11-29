package com.tchalanet.server.core.draw.infra.web.model;

import java.util.List;

/** Payload attendu pour forcer un résultat manuel sur un tirage. */
public record OverrideDrawResultRequest(List<String> numbers, String reason) {}
