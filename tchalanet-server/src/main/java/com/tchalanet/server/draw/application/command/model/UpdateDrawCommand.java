package com.tchalanet.server.draw.application.command.model;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateDrawCommand(UUID tenantId, UUID drawId, LocalDate scheduledDate) {}
