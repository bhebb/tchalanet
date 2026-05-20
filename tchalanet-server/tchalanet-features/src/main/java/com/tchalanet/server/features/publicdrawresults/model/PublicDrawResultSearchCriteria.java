package com.tchalanet.server.features.publicdrawresults.model;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public record PublicDrawResultSearchCriteria(
    List<String> slotKeys,
    String provider,
    LocalDate from,
    LocalDate to,
    Pageable pageable
) {}
