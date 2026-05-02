package com.tchalanet.server.core.drawresult.application.port.out;

import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Criteria for searching draw results (read-only). Used by DrawResult readers.
 */

public record DrawResultsCriteria(
    String slotKey,
    DrawResultStatus status,
    ResultQuality quality,
    LocalDate from,
    LocalDate to,
    Pageable pageable
) {
}
