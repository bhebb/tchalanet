package com.tchalanet.server.core.draw.internal.infra.persistence.adapter;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawProcessingCandidateReaderPort;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DrawProcessingCandidateJdbcAdapter implements DrawProcessingCandidateReaderPort {

    private static final String DRAW_STATUS_CLOSED = "CLOSED";
    private static final String DRAW_STATUS_RESULTED = "RESULTED";

    private final JdbcTemplate jdbc;

    @Override
    public boolean hasApplyCandidates(List<DrawProcessingSlotDate> candidates) {
        var cleanCandidates = normalize(candidates);

        if (cleanCandidates.isEmpty()) {
            return false;
        }

        var params = new ArrayList<Object>();
        var valuesSql = valuesSql(cleanCandidates, params);

        var sql =
            """
            with candidates(result_slot_id, draw_date, expected_occurred_at) as (
              values %s
            )
            select exists (
              select 1
              from candidates c
              join draw_channel dc
                on dc.result_slot_id = c.result_slot_id
              join draw d
                on d.draw_channel_id = dc.id
               and d.draw_date = c.draw_date
              join draw_result dr
                on dr.result_slot_id = c.result_slot_id
               and dr.occurred_at = c.expected_occurred_at
              where d.deleted_at is null
                and dc.deleted_at is null
                and dr.deleted_at is null
                and d.draw_result_id is null
                and d.status = ?
                and coalesce(d.locked, false) = false
                and dr.source = ?
                and dr.quality = ?
                and dr.status in (?, ?, ?)
              limit 1
            )
            """
                .formatted(valuesSql);

        params.add(DRAW_STATUS_CLOSED);
        params.add(DrawSource.EXTERNAL.name());
        params.add(ResultQuality.COMPLETE.name());
        params.add(DrawResultStatus.PROVISIONAL.name());
        params.add(DrawResultStatus.CONFIRMED.name());
        params.add(DrawResultStatus.OVERRIDDEN.name());

        var result = jdbc.queryForObject(sql, Boolean.class, params.toArray());
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean hasSettleCandidates(List<DrawProcessingSlotDate> candidates) {
        var cleanCandidates = normalize(candidates);

        if (cleanCandidates.isEmpty()) {
            return false;
        }

        var params = new ArrayList<Object>();
        var valuesSql = valuesSql(cleanCandidates, params);

        var sql =
            """
            with candidates(result_slot_id, draw_date, expected_occurred_at) as (
              values %s
            )
            select exists (
              select 1
              from candidates c
              join draw_channel dc
                on dc.result_slot_id = c.result_slot_id
              join draw d
                on d.draw_channel_id = dc.id
               and d.draw_date = c.draw_date
              where d.deleted_at is null
                and dc.deleted_at is null
                and d.status = ?
                and d.draw_result_id is not null
                and d.settled_at is null
              limit 1
            )
            """
                .formatted(valuesSql);

        params.add(DRAW_STATUS_RESULTED);

        var result = jdbc.queryForObject(sql, Boolean.class, params.toArray());
        return Boolean.TRUE.equals(result);
    }

    private static List<DrawProcessingSlotDate> normalize(List<DrawProcessingSlotDate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        return candidates.stream()
            .filter(Objects::nonNull)
            .filter(c -> c.resultSlotId() != null)
            .filter(c -> c.drawDate() != null)
            .filter(c -> c.expectedOccurredAt() != null)
            .distinct()
            .toList();
    }

    private static String valuesSql(
        List<DrawProcessingSlotDate> candidates,
        List<Object> params) {

        var values = new ArrayList<String>();

        for (var candidate : candidates) {
            values.add("(?::uuid, ?::date, ?::timestamptz)");
            params.add(candidate.resultSlotId().value());
            params.add(java.sql.Date.valueOf(candidate.drawDate()));
            params.add(Timestamp.from(candidate.expectedOccurredAt()));
        }

        return String.join(", ", values);
    }
}
