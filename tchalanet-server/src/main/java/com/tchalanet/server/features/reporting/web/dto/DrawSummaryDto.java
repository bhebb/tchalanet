package com.tchalanet.server.features.reporting.web.dto;

import java.time.Instant;

public record DrawSummaryDto(String id, String name, Instant closesAt, String status) {}
