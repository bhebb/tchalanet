package com.tchalanet.server.features.stats.cashierdashboard.model;

import java.time.LocalDate;
import java.util.UUID;

public record CashierDashboardStatsCriteria(
    UUID tenantId, UUID cashierId, LocalDate fromDate, LocalDate toDate) {}
