package com.tchalanet.server.features.private_dashboard.block;

import jakarta.annotation.Nullable;

public final class PrivateDashboardSectionMapper {

    private PrivateDashboardSectionMapper() {
    }

    @Nullable
    public static PrivateDashboardSectionType fromRowId(String id) {
        return switch (id) {
            case "overview" -> PrivateDashboardSectionType.OVERVIEW;
            case "kpi_global" -> PrivateDashboardSectionType.KPI_GLOBAL;
            case "kpi_draws" -> PrivateDashboardSectionType.KPI_DRAWS;
            case "kpi_sales" -> PrivateDashboardSectionType.KPI_SALES;
            case "validations" -> PrivateDashboardSectionType.VALIDATIONS;
            case "alerts" -> PrivateDashboardSectionType.ALERTS;
            case "recent_activity" -> PrivateDashboardSectionType.RECENT_ACTIVITY;
            case "shortcuts" -> PrivateDashboardSectionType.SHORTCUTS;
            default -> null;
        };
    }
}

