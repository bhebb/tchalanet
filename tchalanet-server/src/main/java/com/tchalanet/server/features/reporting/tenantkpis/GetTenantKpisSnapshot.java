package com.tchalanet.server.features.reporting.tenantkpis;

import java.time.LocalDate;

public record GetTenantKpisSnapshot(LocalDate fromDate, LocalDate toDate, KpisView kpis) {}
