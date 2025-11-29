package com.tchalanet.server.draw.application.query.model;

import java.time.LocalDate;
import java.util.UUID;

public record ListDrawsQuery(UUID tenantId, String channelCode, LocalDate from, LocalDate to) {}
