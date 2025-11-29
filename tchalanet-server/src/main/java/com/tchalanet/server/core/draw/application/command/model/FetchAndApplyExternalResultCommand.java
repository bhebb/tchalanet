package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.core.draw.domain.model.DrawSource;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FetchAndApplyExternalResultCommand(
    UUID drawId,
    UUID tenantId,
    DrawSource provider,
    Instant fetchedAt,
    Map<String, Object> payload) {}
