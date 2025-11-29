package com.tchalanet.server.draw.application.command.model;

import java.util.UUID;

public record UpdateDrawChannelCommand(
    UUID tenantId, UUID id, String code, String name, boolean active) {}
