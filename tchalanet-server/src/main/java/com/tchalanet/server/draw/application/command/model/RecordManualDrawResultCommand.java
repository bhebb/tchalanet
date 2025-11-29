package com.tchalanet.server.draw.application.command.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecordManualDrawResultCommand(
    UUID drawId,
    UUID tenantId,
    UUID performedBy,
    Instant performedAt,
    List<String> numbersMain,
    List<String> numbersExtra,
    String reason) {}
