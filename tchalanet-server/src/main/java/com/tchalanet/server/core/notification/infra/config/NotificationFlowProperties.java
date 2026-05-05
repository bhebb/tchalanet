package com.tchalanet.server.core.notification.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration des flux de notification métier.
 * Contrôle quels événements génèrent des notifications et vers quels canaux.
 */
@ConfigurationProperties(prefix = "tch.notification.flows")
public record NotificationFlowProperties(
    DrawLifecycleFlow drawLifecycle,
    DrawResultsFlow drawResults,
    ApplyFlow apply,
    SettlementFlow settlement,
    SalesReportsFlow salesReports,
    ClientDeliveryFlow clientDelivery
) {
    public NotificationFlowProperties {
        // Defaults
        if (drawLifecycle == null) {
            drawLifecycle = new DrawLifecycleFlow(true, false, false);
        }
        if (drawResults == null) {
            drawResults = new DrawResultsFlow(true, true, false, List.of("NY", "FL"),
                List.of("NY_MID", "NY_EVE", "FL_MID", "FL_EVE"));
        }
        if (apply == null) {
            apply = new ApplyFlow(true, false, false, false);
        }
        if (settlement == null) {
            settlement = new SettlementFlow(true, false, false);
        }
        if (salesReports == null) {
            salesReports = new SalesReportsFlow(false, false);
        }
        if (clientDelivery == null) {
            clientDelivery = new ClientDeliveryFlow(false, false, false);
        }
    }

    public record DrawLifecycleFlow(
        boolean enabled,
        boolean slackInfoEnabled,
        boolean emailEnabled
    ) {}

    public record DrawResultsFlow(
        boolean enabled,
        boolean slackEnabled,
        boolean emailDetailEnabled,
        List<String> watchedProviders,
        List<String> watchedSlots
    ) {}

    public record ApplyFlow(
        boolean enabled,
        boolean slackInfoEnabled,
        boolean emailOnWarningEnabled,
        boolean emailOnFailureEnabled
    ) {}

    public record SettlementFlow(
        boolean enabled,
        boolean slackInfoEnabled,
        boolean emailAdminEnabled
    ) {}

    public record SalesReportsFlow(
        boolean enabled,
        boolean dailyEmailEnabled
    ) {}

    public record ClientDeliveryFlow(
        boolean enabled,
        boolean ticketSoldEnabled,
        boolean ticketWonEnabled
    ) {}
}

