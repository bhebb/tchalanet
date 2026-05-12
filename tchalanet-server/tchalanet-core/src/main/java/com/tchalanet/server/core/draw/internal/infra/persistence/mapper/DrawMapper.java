package com.tchalanet.server.core.draw.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper bidirectionnel entre l'agrégat {@link Draw} et {@link DrawJpaEntity}.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>Conversion JPA entity → Draw aggregate (toDomain)</li>
 *   <li>Conversion Draw aggregate → JPA entity (toEntity)</li>
 *   <li>Mapping typed IDs ↔ UUID persistence</li>
 *   <li>Gestion des valeurs nullables et defaults (ex: SCHEDULED status)</li>
 * </ul>
 *
 * <p>Note : DrawResultId est nullable (draw peut ne pas avoir de résultat appliqué).
 */
@Component
public class DrawMapper {

    /**
     * Convertit une JPA entity en agrégat Draw.
     *
     * @param jpaEntity entity JPA source
     * @return agrégat Draw ou null si entity null
     */
    public Draw toDomain(DrawJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }

        var drawStatus = jpaEntity.getStatus() == null
            ? DrawStatus.SCHEDULED
            : jpaEntity.getStatus();

        var drawResultId = jpaEntity.getDrawResultId() == null
            ? null
            : DrawResultId.of(jpaEntity.getDrawResultId());

        return new Draw(
            DrawId.of(jpaEntity.getId()),
            TenantId.of(jpaEntity.getTenantId()),
            DrawChannelId.of(jpaEntity.getDrawChannelId()),
            jpaEntity.getDrawDate(),
            jpaEntity.getScheduledAt(),
            jpaEntity.getCutoffAt(),
            drawStatus,
            drawResultId,
            jpaEntity.getOpenedAt(),
            jpaEntity.getClosedAt(),
            jpaEntity.getResultedAt(),
            jpaEntity.getSettledAt(),
            jpaEntity.getCanceledAt(),
            jpaEntity.getCancelReason(),
            jpaEntity.getResultSource(),
            jpaEntity.getResultOverrideReason(),
            jpaEntity.getResultOverriddenAt(),
            jpaEntity.isLocked(),
            jpaEntity.isSystemGenerated()
        );
    }

    /**
     * Convertit un agrégat Draw en JPA entity.
     *
     * @param drawAggregate agrégat Draw source
     * @return JPA entity ou null si aggregate null
     */
    public DrawJpaEntity toEntity(Draw drawAggregate) {
        if (drawAggregate == null) {
            return null;
        }

        var jpaEntity = new DrawJpaEntity();

        jpaEntity.setId(drawAggregate.id().value());
        jpaEntity.setTenantId(drawAggregate.tenantId().value());
        jpaEntity.setDrawChannelId(drawAggregate.drawChannelId().value());

        jpaEntity.setDrawDate(drawAggregate.drawDate());
        jpaEntity.setScheduledAt(drawAggregate.scheduledAt());
        jpaEntity.setCutoffAt(drawAggregate.cutoffAt());

        jpaEntity.setStatus(drawAggregate.status());

        jpaEntity.setDrawResultId(
            drawAggregate.drawResultId() == null
                ? null
                : drawAggregate.drawResultId().value()
        );

        jpaEntity.setOpenedAt(drawAggregate.openedAt());
        jpaEntity.setClosedAt(drawAggregate.closedAt());
        jpaEntity.setResultedAt(drawAggregate.resultedAt());
        jpaEntity.setSettledAt(drawAggregate.settledAt());
        jpaEntity.setCanceledAt(drawAggregate.canceledAt());
        jpaEntity.setCancelReason(drawAggregate.cancelReason());

        jpaEntity.setResultSource(drawAggregate.resultSource());
        jpaEntity.setResultOverrideReason(drawAggregate.resultOverrideReason());
        jpaEntity.setResultOverriddenAt(drawAggregate.resultOverriddenAt());

        jpaEntity.setLocked(drawAggregate.locked());
        jpaEntity.setSystemGenerated(drawAggregate.systemGenerated());

        return jpaEntity;
    }
}
