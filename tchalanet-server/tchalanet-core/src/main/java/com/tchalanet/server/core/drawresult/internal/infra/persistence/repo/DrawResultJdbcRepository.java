package com.tchalanet.server.core.drawresult.internal.infra.persistence.repo;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultProjection;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DrawResultJdbcRepository {

    private final JdbcTemplate jdbc;
    private final JsonUtils jsonUtils;

    // ---------------------------------------------------------
    // 🔍 BASIC LOOKUP
    // ---------------------------------------------------------

    public UUID findByResultSlotIdAndOccurredAt(UUID resultSlotId, Instant occurredAt) {
        var rows = jdbc.query(
            """
                select id
                from draw_result
                where result_slot_id = ?
                  and occurred_at = ?
                  and deleted_at is null
                """,
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            resultSlotId,
            Timestamp.from(occurredAt)
        );

        return rows.isEmpty() ? null : rows.getFirst();
    }

    // ---------------------------------------------------------
    // 📦 VIEW (OPS / ADMIN)
    // ---------------------------------------------------------

    public Optional<DrawResultView> findViewById(UUID id) {
        var rows = jdbc.query(
            baseViewSql("dr.id = ?"),
            this::mapView,
            id
        );
        return rows.stream().findFirst();
    }

    public Optional<DrawResultView> findViewBySlotKeyAndOccurredAt(
        String slotKey,
        Instant occurredAt
    ) {
        var rows = jdbc.query(
            baseViewSql("rs.slot_key = ? and dr.occurred_at = ?"),
            this::mapView,
            slotKey,
            Timestamp.from(occurredAt)
        );
        return rows.stream().findFirst();
    }

    public Optional<DrawResultView> findViewByDrawId(UUID drawId) {
        var sql = """
            select
              dr.id,
              rs.key,
              dr.occurred_at,
              dr.status,
              dr.source,
              dr.quality,
              dr.source_hash,
              dr.fetched_at,
              dr.source_result,
              dr.haiti_result,
              dr.raw_payload,
              dr.override_reason
            from draw_result dr
            join result_slot rs on rs.id = dr.result_slot_id
            join draw d on d.draw_result_id = dr.id
            where dr.deleted_at is null
              and d.deleted_at is null
              and rs.active = true
              and d.id = ?
            """;

        var rows = jdbc.query(sql, this::mapView, drawId);
        return rows.stream().findFirst();
    }

    public List<DrawResultView> findViewsByCriteria(
        String slotKey,
        DrawResultStatus status,
        ResultQuality quality,
        LocalDate from,
        LocalDate to,
        int limit,
        int offset
    ) {
        var sql = new StringBuilder(baseViewSql("1=1"));
        var params = new ArrayList<Object>();

        appendCriteria(sql, params, slotKey, status, quality, from, to);

        sql.append(" order by dr.occurred_at desc limit ? offset ?");
        params.add(limit);
        params.add(offset);

        return jdbc.query(sql.toString(), this::mapView, params.toArray());
    }

    // ---------------------------------------------------------
    // 🔢 COUNT
    // ---------------------------------------------------------

    public long countByCriteria(
        String slotKey,
        DrawResultStatus status,
        ResultQuality quality,
        LocalDate from,
        LocalDate to
    ) {
        var sql = new StringBuilder("""
            select count(1)
            from draw_result dr
            join result_slot rs on rs.id = dr.result_slot_id
            where dr.deleted_at is null
            """);

        var params = new ArrayList<Object>();
        appendCriteria(sql, params, slotKey, status, quality, from, to);

        var count = jdbc.queryForObject(sql.toString(), params.toArray(), Long.class);
        return count == null ? 0L : count;
    }

    // ---------------------------------------------------------
    // 📊 PROJECTION (CROSS-DOMAIN)
    // ---------------------------------------------------------

    public Optional<DrawResultProjection> findProjectionById(UUID id) {
        var rows = jdbc.query(
            projectionSql("dr.id = ?"),
            this::mapProjection,
            id
        );
        return rows.stream().findFirst();
    }

    public Optional<DrawResultProjection> findProjectionBySlotKeyAndOccurredAt(
        String slotKey,
        Instant occurredAt
    ) {
        var rows = jdbc.query(
            projectionSql("rs.key = ? and dr.occurred_at = ?"),
            this::mapProjection,
            slotKey,
            Timestamp.from(occurredAt)
        );
        return rows.stream().findFirst();
    }

    // ---------------------------------------------------------
    // 🧠 CRITERIA BUILDER
    // ---------------------------------------------------------

    private void appendCriteria(
        StringBuilder sql,
        List<Object> params,
        String slotKey,
        DrawResultStatus status,
        ResultQuality quality,
        LocalDate from,
        LocalDate to
    ) {
        if (slotKey != null && !slotKey.isBlank()) {
            sql.append(" and rs.slot_key = ?");
            params.add(slotKey.trim());
        }

        if (status != null) {
            sql.append(" and dr.status = ?");
            params.add(status.name());
        }

        if (quality != null) {
            sql.append(" and dr.quality = ?");
            params.add(quality.name());
        }

        if (from != null) {
            sql.append(" and dr.occurred_at >= ?");
            params.add(Timestamp.from(from.atStartOfDay(ZoneOffset.UTC).toInstant()));
        }

        if (to != null) {
            sql.append(" and dr.occurred_at < ?");
            params.add(Timestamp.from(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
    }

    // ---------------------------------------------------------
    // 🧾 SQL BASE
    // ---------------------------------------------------------

    private String baseViewSql(String where) {
        return """
            select
              dr.id,
              rs.slot_key,
              dr.occurred_at,
              dr.status,
              dr.source,
              dr.quality,
              dr.source_hash,
              dr.fetched_at,
              dr.source_result,
              dr.haiti_result,
              dr.raw_payload,
              dr.override_reason
            from draw_result dr
            join result_slot rs on rs.id = dr.result_slot_id
            where dr.deleted_at is null
              and %s
            """.formatted(where);
    }

    private String projectionSql(String where) {
        return """
            select
              dr.id,
              rs.slot_key,
              dr.occurred_at,
              dr.haiti_result,
              dr.source_result
            from draw_result dr
            join result_slot rs on rs.id = dr.result_slot_id
            where dr.deleted_at is null
              and %s
            """.formatted(where);
    }

    // ---------------------------------------------------------
    // 🔁 MAPPERS
    // ---------------------------------------------------------

    private DrawResultView mapView(ResultSet rs, int rowNum) throws SQLException {

        return new DrawResultView(
            DrawResultId.of(rs.getObject("id", UUID.class)),
            rs.getString("slot_key"),
            rs.getTimestamp("occurred_at").toInstant(),
            DrawResultStatus.valueOf(rs.getString("status")),
            DrawSource.valueOf(rs.getString("source")),
            ResultQuality.valueOf(rs.getString("quality")),
            rs.getString("source_hash"),
            rs.getTimestamp("fetched_at") == null ? null : rs.getTimestamp("fetched_at").toInstant(),
            parseJson(rs.getString("source_result")),
            parseJson(rs.getString("haiti_result")),
            parseJson(rs.getString("raw_payload")),
            rs.getString("override_reason")
        );
    }

    private DrawResultProjection mapProjection(ResultSet rs, int rowNum) throws SQLException {

        var id = DrawResultId.of(rs.getObject("id", UUID.class));
        var slotKey = rs.getString("slot_key");
        var occurredAt = rs.getTimestamp("occurred_at").toInstant();

        var haiti = parseJson(rs.getString("haiti_result"));
        var source = parseJson(rs.getString("source_result"));

        var payload = (haiti != null && !haiti.isNull()) ? haiti : source;

        return new DrawResultProjection(
            id,
            slotKey,
            occurredAt,
            text(payload, "lot1"),
            text(payload, "lot2"),
            text(payload, "lot3"),
            text(payload, "lot4"),
            list(payload, "derived_pairs")
        );
    }

    // ---------------------------------------------------------
    // 🧩 JSON HELPERS
    // ---------------------------------------------------------

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return jsonUtils.parse(raw);
    }

    private static String text(JsonNode node, String key) {
        if (node == null || node.isNull()) return null;
        var value = node.get(key);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static List<String> list(JsonNode node, String key) {
        if (node == null || node.isNull()) return List.of();

        var value = node.get(key);
        if (value == null || !value.isArray()) return List.of();

        var out = new ArrayList<String>();
        value.forEach(item -> {
            if (item != null && item.isTextual()) {
                out.add(item.asText());
            }
        });

        return List.copyOf(out);
    }


    public boolean existsUsableExternalResult(UUID resultSlotId, Instant occurredAt) {
        var count = jdbc.queryForObject(
            """
                select count(1)
                from draw_result dr
                where dr.result_slot_id = ?
                  and dr.occurred_at = ?
                  and dr.deleted_at is null
                  and dr.source = ?
                  and dr.status in (?, ?, ?)
                  and dr.quality = ?
                """,
            Long.class,
            resultSlotId,
            Timestamp.from(occurredAt),
            DrawSource.EXTERNAL.name(),
            DrawResultStatus.PROVISIONAL.name(),
            DrawResultStatus.CONFIRMED.name(),
            DrawResultStatus.OVERRIDDEN.name(),
            ResultQuality.COMPLETE.name()
        );

        return count != null && count > 0;
    }}
