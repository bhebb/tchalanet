package com.tchalanet.server.features.reporting.tenantkpis;

import java.time.LocalDate;

public record GetTenantKpisSnapshotDto(LocalDate fromDate, LocalDate toDate, KpisDto kpis) {}
