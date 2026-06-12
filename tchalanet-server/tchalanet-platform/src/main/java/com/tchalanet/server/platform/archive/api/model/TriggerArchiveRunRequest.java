package com.tchalanet.server.platform.archive.api.model;

import java.time.LocalDate;

public record TriggerArchiveRunRequest(
    String strategy,
    LocalDate periodStart,
    LocalDate periodEnd,
    String reason
) {}
