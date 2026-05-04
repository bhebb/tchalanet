package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.domain.model.*;
import jakarta.persistence.EntityNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class DrawSummaryReaderPersistenceAdapter implements DrawSummaryReaderPort {

    private final JdbcTemplate jdbc;

    private static final String BASE_SELECT_CORE = """
      SELECT d.id AS draw_id,
             d.draw_date,
             d.scheduled_at,
             d.cutoff_at,
             d.status AS draw_status,
             d.locked AS draw_locked,

             dc.id AS channel_id,
             dc.name AS channel_name,
             dc.code AS channel_code,
             dc.active AS channel_active,

             rs.id AS slot_id,
             rs.slot_key AS slot_key,
             rs.label_key AS slot_label,
             rs.timezone AS slot_tz,
             rs.draw_time AS slot_time,
             rs.active AS slot_active,

             dr.id AS result_id,
             dr.occurred_at AS result_at,
             dr.status AS result_status,
             dr.haiti_result AS haiti_result
      FROM draw d
      JOIN draw_channel dc ON d.draw_channel_id = dc.id
      JOIN result_slot rs ON dc.result_slot_id = rs.id
      LEFT JOIN draw_result dr ON d.draw_result_id = dr.id
      WHERE d.deleted_at IS NULL
        AND dc.deleted_at IS NULL
        AND rs.deleted_at IS NULL
      """;

    private static final String BASE_SELECT = """
      SELECT base.*,
             false AS is_next,
             CASE
               WHEN base.draw_status IN ('SCHEDULED', 'OPEN')
                AND base.draw_locked = false
                AND base.channel_active = true
                AND base.slot_active = true
               THEN true
               ELSE false
             END AS is_active
      FROM (
      """ + BASE_SELECT_CORE + """
      ) base
      """;

    @Override
    public Optional<DrawSummary> findById(DrawId drawId) {
        String sql = BASE_SELECT + " AND base.draw_id = ?";
        return jdbc.query(sql, new DrawSummaryRowMapper(), drawId.value()).stream().findFirst();
    }

    @Override
    public DrawSummary getById(DrawId drawId) {
        return findById(drawId)
            .orElseThrow(() -> new EntityNotFoundException("Draw not found"));
    }

    @Override
    public TchPage<DrawSummary> findByCriteria(DrawSearchCriteria criteria, Pageable pageable) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();

        if (criteria != null) {
            if (criteria.resultSlotId() != null) {
                sql.append(" AND base.slot_id = ?");
                params.add(criteria.resultSlotId().value());
            }
            if (criteria.status() != null && !criteria.status().isBlank()) {
                sql.append(" AND base.draw_status = ?");
                params.add(criteria.status());
            }
            if (criteria.from() != null) {
                sql.append(" AND base.draw_date >= ?");
                params.add(criteria.from());
            }
            if (criteria.to() != null) {
                sql.append(" AND base.draw_date <= ?");
                params.add(criteria.to());
            }
        }

        sql.append(" ORDER BY base.scheduled_at DESC");

        return queryPaging(sql, params, pageable);
    }

    @Override
    public TchPage<DrawSummary> listNext(DrawSearchCriteria criteria, Pageable pageable) {
        StringBuilder inner = new StringBuilder(BASE_SELECT_CORE);
        List<Object> params = new ArrayList<>();

        inner.append("""
        AND d.status IN ('SCHEDULED', 'OPEN')
        AND d.locked = false
        AND d.draw_result_id IS NULL
        AND d.scheduled_at >= CURRENT_TIMESTAMP
        AND dc.active = true
        AND rs.active = true
        """);

        if (criteria != null && criteria.resultSlotId() != null) {
            inner.append(" AND rs.id = ?");
            params.add(criteria.resultSlotId().value());
        }

        int limitPerChannel =
            criteria != null && criteria.limitPerChannel() != null
                ? criteria.limitPerChannel()
                : 1;

        StringBuilder sql = new StringBuilder("""
        SELECT ranked.*,
               CASE WHEN ranked.channel_rank = 1 THEN true ELSE false END AS is_next,
               true AS is_active
        FROM (
          SELECT base.*,
                 ROW_NUMBER() OVER (
                   PARTITION BY base.channel_id
                   ORDER BY base.scheduled_at ASC
                 ) AS channel_rank
          FROM (
        """);

        sql.append(inner);

        sql.append("""
          ) base
        ) ranked
        WHERE ranked.channel_rank <= ?
        ORDER BY ranked.scheduled_at ASC
        """);

        params.add(limitPerChannel);

        return queryPaging(sql, params, pageable);
    }

    @Override
    public TchPage<DrawSummary> listLatestWithResults(DrawSearchCriteria criteria, Pageable pageable) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();

        sql.append(" AND base.result_id IS NOT NULL");

        if (criteria != null && criteria.resultSlotKeys() != null && !criteria.resultSlotKeys().isEmpty()) {
            sql.append(" AND base.slot_key = ANY (?)");
            params.add(criteria.resultSlotKeys().toArray(String[]::new));
        }

        sql.append(" ORDER BY base.draw_date DESC, base.scheduled_at DESC");

        return queryPaging(sql, params, pageable);
    }

    private TchPage<DrawSummary> queryPaging(
        StringBuilder sql,
        List<Object> params,
        Pageable pageable
    ) {
        String countSql = "SELECT count(*) FROM (" + sql + ") t";
        Long totalElements = jdbc.queryForObject(countSql, Long.class, params.toArray());

        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getPageSize());
        params.add(pageable.getOffset());

        List<DrawSummary> content =
            jdbc.query(sql.toString(), new DrawSummaryRowMapper(), params.toArray());

        long total = totalElements != null ? totalElements : 0;
        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());

        return new TchPage<>(
            content,
            pageable.getPageNumber(),
            pageable.getPageSize(),
            total,
            totalPages,
            (long) (pageable.getPageNumber() + 1) * pageable.getPageSize() >= total,
            (long) (pageable.getPageNumber() + 1) * pageable.getPageSize() < total,
            pageable.getPageNumber() > 0
        );
    }

    private static class DrawSummaryRowMapper implements RowMapper<DrawSummary> {

        @Override
        public DrawSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
            DrawChannelSummary channel = new DrawChannelSummary(
                DrawChannelId.of((UUID) rs.getObject("channel_id")),
                rs.getString("channel_name"),
                rs.getString("channel_code")
            );

            ResultSlotSummary slot = new ResultSlotSummary(
                ResultSlotId.of((UUID) rs.getObject("slot_id")),
                rs.getString("slot_key"),
                rs.getString("slot_label"),
                rs.getString("slot_tz"),
                rs.getObject("slot_time", LocalTime.class)
            );

            HaitiDrawResultSummary result = null;
            UUID resultId = (UUID) rs.getObject("result_id");
            if (resultId != null) {
                result = new HaitiDrawResultSummary(
                    DrawResultId.of(resultId),
                    rs.getTimestamp("result_at").toInstant(),
                    rs.getString("result_status"),
                    rs.getObject("haiti_result", JsonNode.class)
                );
            }

            DrawStatus status = DrawStatus.valueOf(rs.getString("draw_status"));

            return new DrawSummary(
                DrawId.of((UUID) rs.getObject("draw_id")),
                channel,
                slot,
                rs.getObject("draw_date", LocalDate.class),
                rs.getTimestamp("scheduled_at").toInstant(),
                rs.getTimestamp("cutoff_at").toInstant(),
                status,
                rs.getBoolean("is_next"),
                rs.getBoolean("is_active"),
                result
            );
        }
    }
}
