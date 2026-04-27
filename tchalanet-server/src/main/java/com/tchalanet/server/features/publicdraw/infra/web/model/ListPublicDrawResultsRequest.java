package com.tchalanet.server.features.publicdraw.infra.web.model;

import java.time.LocalDate;

public record ListPublicDrawResultsRequest(
    String slotKey, LocalDate from, LocalDate to, int page, int size) {}
