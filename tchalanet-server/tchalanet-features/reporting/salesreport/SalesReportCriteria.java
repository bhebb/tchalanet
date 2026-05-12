package com.tchalanet.server.features.reporting.salesreport;

import java.time.LocalDate;
import java.util.UUID;

/** Paramètres de la requête de rapport de ventes par période et par jeu. */
public record SalesReportCriteria(
    UUID tenantId, LocalDate fromDate, LocalDate toDate, String gameCode) {}
