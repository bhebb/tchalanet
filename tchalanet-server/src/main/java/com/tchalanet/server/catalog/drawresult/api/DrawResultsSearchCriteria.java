package com.tchalanet.server.catalog.drawresult.api;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public record DrawResultsSearchCriteria(
    String provider, // optional (NY/FL/GA/TX)
    String slotKey, // optional (NY_MID)
    LocalDate from, // optional (date locale du slot)
    LocalDate to, // optional (date locale du slot)
    Pageable pageable) {}
