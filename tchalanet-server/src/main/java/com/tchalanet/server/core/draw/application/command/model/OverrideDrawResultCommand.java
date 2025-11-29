package com.tchalanet.server.core.draw.application.command.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OverrideDrawResultCommand(
    UUID drawId,
    UUID tenantId,
    UUID adminId,
    Instant overriddenAt,
    List<String> numbersMain,
    List<String> numbersExtra,
    String reason) {}
