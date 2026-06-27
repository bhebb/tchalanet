package com.tchalanet.server.core.drawresult.internal.infra.persistence.adapter;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.internal.infra.cache.DrawResultCacheEvictor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.tchalanet.server.core.drawresult.internal.infra.persistence.DrawResultJpaEntity;
import com.tchalanet.server.core.drawresult.internal.infra.persistence.repo.DrawResultJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultWriterJpaAdapter implements DrawResultWriterPort {
    private final JsonUtils jsonUtils;
    private final DrawResultCacheEvictor cacheEvictor;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final DrawResultJpaRepository drawResultJpaRepository;


    @Override
    @TchTx
    public UpsertResult upsert(
        ResultSlotId resultSlotId,
        LocalDate resultDate,
        Instant occurredAt,
        JsonNode sourceResult,
        JsonNode haitiResult,
        JsonNode rawPayload,
        String status,
        String source,
        JsonNode flags,
        String quality,
        String sourceHash,
        String overrideReason,
        boolean force
    ) {
        var now = Instant.now(clock);

        var existing = drawResultJpaRepository
            .findByResultSlotIdAndResultDateAndDeletedAtIsNull(resultSlotId.value(), resultDate)
            .orElse(null);

        if (existing != null && isProtected(existing) && !force) {
            return new UpsertResult(
                DrawResultId.of(existing.getId()),
                false,
                false,
                existing.getStatus() == DrawResultStatus.CONFIRMED,
                existing.getStatus() == DrawResultStatus.OVERRIDDEN);
        }

        var created = existing == null;
        var entity = created ? new DrawResultJpaEntity() : existing;
        if (created) {
            entity.setId(idGenerator.newUuid());
            entity.setResultSlotId(resultSlotId.value());
            entity.setResultDate(resultDate);
        }

        entity.setOccurredAt(occurredAt);
        entity.setSourceResult(requiredJson(sourceResult));
        entity.setHaitiResult(requiredJson(haitiResult));
        entity.setRawPayload(nullableJson(rawPayload));
        entity.setStatus(DrawResultStatus.valueOf(status));
        entity.setSource(DrawSource.valueOf(source));
        entity.setFlags(jsonOrEmpty(flags));
        entity.setQuality(quality == null ? null : ResultQuality.valueOf(quality));
        entity.setSourceHash(sourceHash);
        entity.setOverrideReason(overrideReason);
        entity.setFetchedAt(now);

        var saved = drawResultJpaRepository.saveAndFlush(entity);
        AfterCommit.run(cacheEvictor::evictAll);
        return new UpsertResult(DrawResultId.of(saved.getId()), created, !created, false, false);
    }

    private boolean isProtected(DrawResultJpaEntity entity) {
        return entity.getStatus() == DrawResultStatus.CONFIRMED
            || entity.getStatus() == DrawResultStatus.OVERRIDDEN;
    }

    private JsonNode requiredJson(JsonNode node) {
        return node == null || node.isNull() ? jsonUtils.toJsonNode("{}") : node;
    }

    private JsonNode nullableJson(JsonNode node) {
        return node == null || node.isNull() ? null : node;
    }

    private JsonNode jsonOrEmpty(JsonNode node) {
        return node == null || node.isNull() ? jsonUtils.toJsonNode("{}") : node;
    }

    @Override
    public void confirmProvisional(DrawResultId drawResultId, Instant confirmedAt) {
        var entity = drawResultJpaRepository.findByIdAndDeletedAtIsNull(drawResultId.value()).orElse(null);
        if (entity == null || entity.getStatus() != DrawResultStatus.PROVISIONAL) {
            log.warn("drawresult.confirm_provisional_noop drawResultId={}", drawResultId);
            return;
        }

        entity.setStatus(DrawResultStatus.CONFIRMED);
        entity.setUpdatedAt(confirmedAt);
        drawResultJpaRepository.saveAndFlush(entity);
        log.info("drawresult.confirmed_provisional drawResultId={}", drawResultId);
        AfterCommit.run(cacheEvictor::evictAll);
    }

    @Override
    public void markAsOverridden(DrawResultId drawResultId, String reason, Instant overriddenAt) {
        var entity = drawResultJpaRepository.findByIdAndDeletedAtIsNull(drawResultId.value()).orElse(null);
        if (entity == null || entity.getStatus() == DrawResultStatus.OVERRIDDEN) {
            log.warn("drawresult.mark_overridden_noop drawResultId={}", drawResultId);
            return;
        }

        entity.setStatus(DrawResultStatus.OVERRIDDEN);
        entity.setOverrideReason(reason);
        entity.setUpdatedAt(overriddenAt);
        drawResultJpaRepository.saveAndFlush(entity);
        log.info("drawresult.marked_overridden drawResultId={} reason={}", drawResultId, reason);
        AfterCommit.run(cacheEvictor::evictAll);
    }
}
