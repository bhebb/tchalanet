package com.tchalanet.server.reporting.web.dto;

import java.time.Instant;

public record DrawSummaryDto(String id, String name, Instant closesAt, String status) {}
