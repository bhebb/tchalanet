package com.tchalanet.server.core.draw.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.mapper.DrawMapper;
import com.tchalanet.server.core.draw.infra.persistence.repo.DrawJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DrawLookupPersistenceAdapter implements DrawLookupPort {

    private final DrawJpaRepository jpa;
    private final DrawMapper mapper;
    private final TchContextResolver contextResolver;

    @Override
    public Optional<Draw> findById(DrawId drawId) {
        Objects.requireNonNull(drawId, "drawId is required");
        var tenantUuid = currentTenantUuid();

        return jpa.findByTenantIdAndIdAndDeletedAtIsNull(
                tenantUuid,
                drawId.value()
            )
            .map(mapper::toDomain);
    }

    @Override
    public Draw getById(DrawId drawId) {
        return findById(drawId)
            .orElseThrow(() -> new EntityNotFoundException("Draw not found: " + drawId));
    }

    @Override
    public Draw getByIdForUpdate(DrawId drawId) {
        Objects.requireNonNull(drawId, "drawId is required");
        var tenantUuid = currentTenantUuid();

        return jpa.findByTenantIdAndIdAndDeletedAtIsNullForUpdate(tenantUuid, drawId.value())
            .map(mapper::toDomain)
            .orElseThrow(() -> new EntityNotFoundException("Draw not found: " + drawId));
    }

    @Override
    public boolean existsSettledDrawForResult(DrawResultId drawResultId) {
        Objects.requireNonNull(drawResultId, "drawResultId is required");
        var tenantUuid = currentTenantUuid();

        return jpa.existsByTenantIdAndDrawResultIdAndStatusAndDeletedAtIsNull(
            tenantUuid,
            drawResultId.value(),
            DrawStatus.SETTLED
        );
    }

    private UUID currentTenantUuid() {
        return contextResolver.currentOrThrow().effectiveTenantIdRequired().value();
    }

}
