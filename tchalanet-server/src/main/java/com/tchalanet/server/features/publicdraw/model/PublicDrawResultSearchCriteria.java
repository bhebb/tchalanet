package com.tchalanet.server.features.publicdraw.model;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public record PublicDrawResultSearchCriteria(
    String slotKey, String provider, LocalDate from, LocalDate to, Pageable pageable) {}
