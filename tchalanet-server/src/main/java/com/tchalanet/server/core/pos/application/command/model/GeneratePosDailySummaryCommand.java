package com.tchalanet.server.core.pos.application.command.model;

import java.util.UUID;
import java.time.LocalDate;

public record GeneratePosDailySummaryCommand(UUID tenantId, UUID outletId, LocalDate date) {}

