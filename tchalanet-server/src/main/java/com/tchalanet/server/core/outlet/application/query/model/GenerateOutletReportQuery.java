package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;
import java.time.LocalDate;

public record GenerateOutletReportQuery(UUID tenantId, UUID outletId, LocalDate from, LocalDate to) implements Query<java.nio.file.Path> {}

