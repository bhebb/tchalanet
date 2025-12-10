package com.tchalanet.server.features.stats.cashier_dashboard.application;

import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyEntity;
import com.tchalanet.server.features.stats.aggregates.persistence.StatsDailyJpaRepository;
import com.tchalanet.server.features.stats.cashier_dashboard.dto.CashierDailySalesPointDto;
import com.tchalanet.server.features.stats.cashier_dashboard.dto.CashierDashboardStatsResponse;
import com.tchalanet.server.features.stats.cashier_dashboard.dto.CashierSummaryCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nouveau use case "propre" pour les stats du dashboard caissier.
 * - Lit UNIQUEMENT dans stats_daily (dimension_type = 'cashier').
 * - Ne fait pas de gros GROUP BY sur ticket/session.
 * - Utilise des repos de lecture pour les noms caissier / outlet et la session ouverte.
 */
@Component
@RequiredArgsConstructor
public class CashierDashboardStatsUseCase {

    private final StatsDailyJpaRepository statsDailyRepo;
    private final CashierReadRepository cashierReadRepository;
    private final CashierSessionReadRepository cashierSessionReadRepository;
    private final Clock clock;

    public CashierDashboardStatsResponse handle(CashierDashboardStatsQuery query) {
        UUID tenantId = query.tenantId();
        UUID cashierId = query.cashierId();

        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = query.fromDate() != null ? query.fromDate() : today.minusDays(6);
        LocalDate toDate = query.toDate() != null ? query.toDate() : today;

        // 1) Récupérer les lignes stats_daily pour ce caissier
        List<StatsDailyEntity> rows = statsDailyRepo
            .findByCashierAndDateRange(cashierId, fromDate, toDate);

        long ticketsTotal = 0L;
        long stakeTotalCents = 0L;
        long winningsTotalCents = 0L;
        long netTotalCents = 0L;

        List<CashierDailySalesPointDto> daily = new ArrayList<>();

        for (StatsDailyEntity row : rows) {
            ticketsTotal += row.getTicketsCount();
            stakeTotalCents += row.getStakeSumCents();
            winningsTotalCents += row.getWinningsSumCents();
            netTotalCents += row.getNetRevenueCents();

            daily.add(new CashierDailySalesPointDto(
                row.getRefDate(),
                centsToMoney(row.getStakeSumCents()),
                centsToMoney(row.getWinningsSumCents())
            ));
        }

        // 2) Infos caissier + outlet
        CashierInfoProjection info = cashierReadRepository
            .findInfoById(tenantId, cashierId)
            .orElseGet(() -> new CashierInfoProjection(
                cashierId,
                "Cashier " + cashierId,
                null,
                null
            ));

        boolean hasOpenSession = cashierSessionReadRepository
            .hasOpenSession(tenantId, cashierId);

        CashierSummaryCardDto summary = new CashierSummaryCardDto(
            info.cashierId(),
            info.cashierName(),
            info.outletId(),
            info.outletName(),
            ticketsTotal,
            centsToMoney(stakeTotalCents),
            centsToMoney(winningsTotalCents),
            centsToMoney(netTotalCents),
            hasOpenSession
        );

        // Pour le moment, pas de breakdown par jeu → tu pourras l’ajouter plus tard
        // via stats_draw ou une autre table d’agrégats.
        return new CashierDashboardStatsResponse(
            fromDate,
            toDate,
            summary,
            List.of(),      // game breakdown
            daily
        );
    }

    /**
     * Pour l’instant, ces méthodes restaient dans ton interface.
     * On les garde, mais on les branche sur des agrégats (stats_daily).
     * À implémenter avec un repo custom ou une requête native selon ton schéma.
     */
    public List<UUID> getTopCashierIds(UUID tenantId, LocalDate from, LocalDate to, int limit) {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = from != null ? from : today.minusDays(6);
        LocalDate toDate = to != null ? to : today;

        List<CashierAggregateRow> rows =
            statsDailyRepo.findTopCashiersByTenantAndDateRange(tenantId, fromDate, toDate, limit);

        return rows.stream()
            .map(CashierAggregateRow::cashierId)
            .toList();
    }

    public List<CashierDashboardStatsResponse> getTopCashierSummaries(UUID tenantId,
                                                                      LocalDate from,
                                                                      LocalDate to,
                                                                      int limit) {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = from != null ? from : today.minusDays(6);
        LocalDate toDate = to != null ? to : today;

        List<CashierAggregateRow> rows =
            statsDailyRepo.findTopCashiersByTenantAndDateRange(tenantId, fromDate, toDate, limit);

        List<CashierDashboardStatsResponse> result = new ArrayList<>();

        for (CashierAggregateRow row : rows) {
            CashierInfoProjection info = cashierReadRepository
                .findInfoById(tenantId, row.cashierId())
                .orElseGet(() -> new CashierInfoProjection(
                    row.cashierId(),
                    "Cashier " + row.cashierId(),
                    row.outletId(),
                    null
                ));

            boolean hasOpenSession = cashierSessionReadRepository
                .hasOpenSession(tenantId, row.cashierId());

            CashierSummaryCardDto summary = new CashierSummaryCardDto(
                info.cashierId(),
                info.cashierName(),
                info.outletId(),
                info.outletName(),
                row.ticketsCount(),
                centsToMoney(row.stakeSumCents()),
                centsToMoney(row.winningsSumCents()),
                centsToMoney(row.netRevenueCents()),
                hasOpenSession
            );

            result.add(new CashierDashboardStatsResponse(
                fromDate,
                toDate,
                summary,
                List.of(),
                List.of()   // pas de courbe détaillée dans ce cas
            ));
        }

        return result;
    }

    private BigDecimal centsToMoney(long cents) {
        // 2 décimales (100 = 1.00)
        return BigDecimal.valueOf(cents, 2);
    }
}
