package com.tchalanet.server.features.publicdraw.model;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public record PublicDrawResultSearchCriteria(
    String slotKey,
    String provider,
    LocalDate from,
    LocalDate to,
    Pageable pageable
) {}
