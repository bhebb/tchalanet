package com.tchalanet.server.core.drawresult.infra.web.ops.model;

import java.time.LocalDate;
import java.util.List;

public record RecordManualDrawResultRequest(
    String tenantId,
    LocalDate drawDate,
    String channelCode,
    String recordedBy,
    String notes,
    String pick3,
    String pick4,
    List<String> rawPayload) {}
