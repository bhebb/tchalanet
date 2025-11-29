package com.tchalanet.server.core.draw.application.query.model;

import java.time.LocalDate;
import java.util.UUID;

public record ListDrawResultsQuery(
    UUID tenantId, String channelCode, LocalDate from, LocalDate to) {}
