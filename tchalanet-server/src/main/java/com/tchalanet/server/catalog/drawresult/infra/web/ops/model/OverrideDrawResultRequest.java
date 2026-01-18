package com.tchalanet.server.catalog.drawresult.infra.web.ops.model;

import java.time.LocalDate;
import java.util.List;

public record OverrideDrawResultRequest(
    String tenantId,
    LocalDate drawDate,
    String channelCode,
    String reason,
    String pick3,
    String pick4,
    List<String> rawPayload) {}
