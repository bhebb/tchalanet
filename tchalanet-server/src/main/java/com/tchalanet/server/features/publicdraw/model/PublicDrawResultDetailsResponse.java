package com.tchalanet.server.features.publicdraw.model;

import java.time.Instant;
import java.time.LocalDate;

public record PublicDrawResultDetailsResponse(
    PublicDrawResultItemResponse result,
    Instant nextScheduledAt, // UTC
    LocalDate nextDrawDate, // date locale slot (optionnel)
    String nextDrawTime // "14:30" (optionnel)
    ) {}
