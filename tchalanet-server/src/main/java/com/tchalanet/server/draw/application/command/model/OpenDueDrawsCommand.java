package com.tchalanet.server.draw.application.command.model;

import java.time.LocalDate;
import java.util.UUID;

public record OpenDueDrawsCommand(UUID tenantId, LocalDate date, int chunkSize) {}
