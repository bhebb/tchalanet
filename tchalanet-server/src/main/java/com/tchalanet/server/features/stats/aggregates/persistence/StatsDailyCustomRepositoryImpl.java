package com.tchalanet.server.features.stats.aggregates.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public class StatsDailyCustomRepositoryImpl implements StatsDailyCustomRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void upsertAndIncrement(String dimensionType, UUID dimensionId, LocalDate refDate, long ticketsDelta, long cancelledDelta, long stakeDelta, long winningsDelta, long payoutsDelta, long sessionsOpenedDelta, long sessionsClosedDelta) {
        String sql = "INSERT INTO stats_daily (id, dimension_type, dimension_id, ref_date, tickets_count, tickets_cancelled_count, stake_sum_cents, winnings_sum_cents, net_revenue_cents, payouts_count, sessions_opened_count, sessions_closed_count, created_at, updated_at) " +
                "VALUES (gen_random_uuid(), :dimensionType, :dimensionId, :refDate, :ticketsDelta, :cancelledDelta, :stakeDelta, :winningsDelta, :netDelta, :payoutsDelta, :sessionsOpenedDelta, :sessionsClosedDelta, now(), now()) " +
                "ON CONFLICT (dimension_type, dimension_id, ref_date) DO UPDATE SET " +
                "tickets_count = stats_daily.tickets_count + EXCLUDED.tickets_count, " +
                "tickets_cancelled_count = stats_daily.tickets_cancelled_count + EXCLUDED.tickets_cancelled_count, " +
                "stake_sum_cents = stats_daily.stake_sum_cents + EXCLUDED.stake_sum_cents, " +
                "winnings_sum_cents = stats_daily.winnings_sum_cents + EXCLUDED.winnings_sum_cents, " +
                "net_revenue_cents = stats_daily.net_revenue_cents + EXCLUDED.net_revenue_cents, " +
                "payouts_count = stats_daily.payouts_count + EXCLUDED.payouts_count, " +
                "sessions_opened_count = stats_daily.sessions_opened_count + EXCLUDED.sessions_opened_count, " +
                "sessions_closed_count = stats_daily.sessions_closed_count + EXCLUDED.sessions_closed_count, " +
                "updated_at = now(), version = stats_daily.version + 1";

        long netDelta = stakeDelta - winningsDelta;

        em.createNativeQuery(sql)
                .setParameter("dimensionType", dimensionType)
                .setParameter("dimensionId", dimensionId)
                .setParameter("refDate", refDate)
                .setParameter("ticketsDelta", ticketsDelta)
                .setParameter("cancelledDelta", cancelledDelta)
                .setParameter("stakeDelta", stakeDelta)
                .setParameter("winningsDelta", winningsDelta)
                .setParameter("netDelta", netDelta)
                .setParameter("payoutsDelta", payoutsDelta)
                .setParameter("sessionsOpenedDelta", sessionsOpenedDelta)
                .setParameter("sessionsClosedDelta", sessionsClosedDelta)
                .executeUpdate();
    }
}

