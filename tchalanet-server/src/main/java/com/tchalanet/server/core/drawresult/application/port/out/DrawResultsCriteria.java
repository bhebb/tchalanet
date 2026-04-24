package com.tchalanet.server.core.drawresult.application.port.out;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Criteria for searching draw results (read-only). Used by DrawResult readers.
 */
public record DrawResultsCriteria(
    String provider,
    String slotKey,
    LocalDate from,
    LocalDate to,
    Pageable pageable
) {}
