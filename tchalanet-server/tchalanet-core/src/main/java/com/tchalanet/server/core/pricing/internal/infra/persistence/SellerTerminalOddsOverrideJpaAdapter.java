package com.tchalanet.server.core.pricing.internal.infra.persistence;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.SellerTerminalOddsOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideReaderPort;
import com.tchalanet.server.core.pricing.internal.application.port.out.SellerTerminalOddsOverrideWriterPort;
import com.tchalanet.server.core.pricing.internal.domain.SellerTerminalOddsOverride;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SellerTerminalOddsOverrideJpaAdapter
    implements SellerTerminalOddsOverrideReaderPort, SellerTerminalOddsOverrideWriterPort {

    private final SellerTerminalOddsOverrideJpaRepository repo;

    @Override
    public Optional<SellerTerminalOddsOverride> findById(SellerTerminalOddsOverrideId id) {
        return repo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<SellerTerminalOddsOverride> findActiveBySellerTerminal(
        TenantId tenantId, SellerTerminalId sellerTerminalId
    ) {
        return repo.findActiveBySellerTerminal(tenantId.value(), sellerTerminalId.value())
            .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SellerTerminalOddsOverride> findActiveByNaturalKey(
        TenantId tenantId, SellerTerminalId sellerTerminalId,
        String gameCode, String betType, Short betOption
    ) {
        return repo.findActiveByNaturalKey(
            tenantId.value(), sellerTerminalId.value(), gameCode, betType, betOption
        ).map(this::toDomain);
    }

    @Override
    public SellerTerminalOddsOverride save(SellerTerminalOddsOverride o) {
        var entity = repo.findById(o.id().value()).orElseGet(SellerTerminalOddsOverrideJpaEntity::new);
        toEntity(o, entity);
        return toDomain(repo.save(entity));
    }

    @Override
    public void delete(SellerTerminalOddsOverride o) {
        repo.findById(o.id().value()).ifPresent(e -> {
            toEntity(o, e);
            repo.save(e);
        });
    }

    private SellerTerminalOddsOverride toDomain(SellerTerminalOddsOverrideJpaEntity e) {
        return new SellerTerminalOddsOverride(
            SellerTerminalOddsOverrideId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            SellerTerminalId.of(e.getSellerTerminalId()),
            e.getGameCode(), e.getBetType(), e.getBetOption(),
            e.getOdds(), e.isActive(),
            e.getEffectiveFrom(), e.getEffectiveTo(), e.getReason(),
            e.getCreatedAt(),
            UserId.nullableOf(e.getCreatedBy()),
            e.getUpdatedAt(),
            UserId.nullableOf(e.getUpdatedBy()),
            e.getDeletedAt());
    }

    private void toEntity(SellerTerminalOddsOverride o, SellerTerminalOddsOverrideJpaEntity e) {
        e.setId(o.id().value());
        e.setTenantId(o.tenantId().value());
        e.setSellerTerminalId(o.sellerTerminalId().value());
        e.setGameCode(o.gameCode());
        e.setBetType(o.betType());
        e.setBetOption(o.betOption());
        e.setOdds(o.odds());
        e.setActive(o.active());
        e.setEffectiveFrom(o.effectiveFrom());
        e.setEffectiveTo(o.effectiveTo());
        e.setReason(o.reason());
        e.setCreatedAt(o.createdAt());
        e.setCreatedBy(o.createdBy() != null ? o.createdBy().value() : null);
        e.setUpdatedAt(o.updatedAt());
        e.setUpdatedBy(o.updatedBy() != null ? o.updatedBy().value() : null);
        e.setDeletedAt(o.deletedAt());
    }
}
