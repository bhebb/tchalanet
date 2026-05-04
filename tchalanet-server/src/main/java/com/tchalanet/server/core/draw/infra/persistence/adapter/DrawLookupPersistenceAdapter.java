package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummaryView;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawLookupJdbcRepository;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DrawLookupPersistenceAdapter implements DrawLookupPort {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private final DrawLookupJdbcRepository lookupRepo;
    private final DrawJpaRepository jpa;
    private final DrawMapper mapper;
    private final DrawResultReaderPort drawResultReader;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Override
    public Optional<DrawId> findDrawIdBySlotId(
        TenantId tenantId,
        LocalDate drawDate,
        ResultSlotId resultSlotId
    ) {
        if (tenantId == null || drawDate == null || resultSlotId == null) {
            return Optional.empty();
        }

        var id = lookupRepo.findDrawIdBySlotId(
            tenantId.value(),
            drawDate,
            resultSlotId.value()
        );

        return Optional.ofNullable(DrawId.nullableOf(id));
    }

    @Override
    public Optional<Draw> findById(DrawId drawId) {
        Objects.requireNonNull(drawId, "drawId is required");
        return jpa.findById(drawId.value()).map(mapper::toDomain);
    }

    @Override
    public Draw getById(DrawId drawId) {
        return findById(drawId)
            .orElseThrow(() -> new EntityNotFoundException("Draw not found"));
    }

    @Override
    public List<DrawSummaryView> findByCriteria(DrawSearchCriteria criteria) {
        if (criteria == null || criteria.tenantId() == null) {
            return List.of();
        }

        UUID tenantUuid = criteria.tenantId().value();

        Instant fromInstant = criteria.from() == null
            ? Instant.EPOCH
            : criteria.from().atStartOfDay(UTC).toInstant();

        Instant toInstant = criteria.to() == null
            ? Instant.ofEpochSecond(Long.MAX_VALUE)
            : criteria.to().plusDays(1).atStartOfDay(UTC).toInstant().minusNanos(1);

        var rows = jpa.findSummariesWithChannelAndResult(tenantUuid, fromInstant, toInstant).stream()
            .filter(e -> {
                String code = criteria.channelCode();
                if (code == null || code.isBlank()) return true;
                var ch = e.getDrawChannel();
                return ch != null && code.equalsIgnoreCase(ch.getCode());
            })
            .map(e -> toSummary(e, UTC))
            .toList();

        if (rows.isEmpty()) {
            return List.of();
        }

        var mutable = new java.util.ArrayList<>(rows);
        var first = mutable.getFirst();

        mutable.set(0, new DrawSummaryView(
            first.id(),
            first.channelCode(),
            first.channelName(),
            first.scheduledAt(),
            first.cutoffTime(),
            first.status(),
            true,
            first.active(),
            first.lastResult()
        ));

        return mutable;
    }

    @Override
    public boolean existsSettledDrawForResult(DrawResultId drawResultId) {
        Objects.requireNonNull(drawResultId, "drawResultId is required");

        return jpa.existsByDrawResultIdAndStatusAndDeletedAtIsNull(
            drawResultId.value(),
            DrawStatus.SETTLED
        );
    }

    @Override
    public List<DrawSummaryView> findByDrawResultId(DrawResultId drawResultId) {
        Objects.requireNonNull(drawResultId, "drawResultId is required");

        return jpa.findByDrawResultIdAndDeletedAtIsNull(drawResultId.value()).stream()
            .map(e -> toSummary(e, UTC))
            .toList();
    }

    @Override
    public List<DrawSummaryView> findResultedWithProvisionalOlderThan(Duration duration) {
        Objects.requireNonNull(duration, "duration is required");

        Instant threshold = clock.instant().minus(duration);

        String sql = """
            SELECT d.id
            FROM draw d
            JOIN draw_result dr ON dr.id = d.draw_result_id
            WHERE d.status = 'RESULTED'
              AND dr.status = 'PROVISIONAL'
              AND d.resulted_at < ?
              AND d.deleted_at IS NULL
            """;

        List<UUID> ids = jdbc.queryForList(
            sql,
            UUID.class,
            Timestamp.from(threshold)
        );

        if (ids.isEmpty()) {
            return List.of();
        }

        return jpa.findAllById(ids).stream()
            .map(e -> toSummary(e, UTC))
            .toList();
    }

    private DrawSummaryView toSummary(DrawJpaEntity e, ZoneId fallbackZone) {
        var ch = e.getDrawChannel();

        String chCode = ch == null ? null : ch.getCode();
        String chName = ch == null ? null : (ch.getName() == null ? ch.getCode() : ch.getName());
        boolean active = ch != null && ch.isActive();

        ZoneId zone = fallbackZone;
        try {
            if (ch != null && ch.getTimezone() != null && !ch.getTimezone().isBlank()) {
                zone = ZoneId.of(ch.getTimezone());
            }
        } catch (Exception ignored) {
            zone = fallbackZone;
        }

        ZonedDateTime scheduled = e.getScheduledAt() == null
            ? null
            : ZonedDateTime.ofInstant(e.getScheduledAt(), zone);

        ZonedDateTime cutoff = e.getCutoffAt() == null
            ? null
            : ZonedDateTime.ofInstant(e.getCutoffAt(), zone);

        DrawStatus status = e.getStatus() == null ? DrawStatus.SCHEDULED : e.getStatus();

        DrawResult dr = null;
        if (e.getDrawResultId() != null) {
            dr = drawResultReader.getById(DrawResultId.of(e.getDrawResultId()));
        }

        return new DrawSummaryView(
            DrawId.of(e.getId()),
            chCode,
            chName,
            scheduled,
            cutoff,
            status,
            false,
            active,
            dr != null ? dr.haitiResult() : null
        );
    }
}
