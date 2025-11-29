package com.tchalanet.server.draw.application.command.model;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDrawCommand(UUID tenantId, String channelCode, LocalDate scheduledDate) {}
