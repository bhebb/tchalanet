package com.tchalanet.server.core.drawresult.internal.application.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public record DrawResultPersistPayload(
    JsonNode sourceResult,
    ObjectNode haitiResult,
    ObjectNode rawPayload,
    ObjectNode flags,
    String quality,
    String sourceHash
) {}
