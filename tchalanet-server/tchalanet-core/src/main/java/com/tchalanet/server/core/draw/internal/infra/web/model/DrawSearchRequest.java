package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.id.ResultSlotId;
import java.time.LocalDate;

public record DrawSearchRequest(
    ResultSlotId resultSlotId,
    String status,
    LocalDate from,
    LocalDate to
) {}
