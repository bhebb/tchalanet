package com.tchalanet.server.core.draw.infra.web.model;

import java.time.LocalDate;

public record ListPublicDrawResultsRequest(
    String channelCode, LocalDate from, LocalDate to, int page, int size) {}
