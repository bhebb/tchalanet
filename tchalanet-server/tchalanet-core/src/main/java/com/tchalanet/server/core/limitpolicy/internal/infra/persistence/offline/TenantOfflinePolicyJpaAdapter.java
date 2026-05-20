package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.offline;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.offline.TenantOfflinePolicyReaderPort;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.offline.TenantOfflinePolicyWriterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantOfflinePolicyJpaAdapter
    implements TenantOfflinePolicyReaderPort, TenantOfflinePolicyWriterPort {

    private final TenantOfflinePolicyJpaRepository repo;
    private final IdGenerator idGenerator;

    @Override
    public Optional<OfflineLimitPolicy> findByTenantId(TenantId tenantId) {
        return repo.findByTenantId(tenantId.value()).map(TenantOfflinePolicyJpaAdapter::toDomain);
    }

    @Override
    public OfflineLimitPolicy upsert(TenantId tenantId, OfflineLimitPolicy policy) {
        var entity = repo.findByTenantId(tenantId.value()).orElseGet(() -> {
            var e = new TenantOfflinePolicyJpaEntity();
            e.setId(idGenerator.newUuid());
            e.setTenantId(tenantId.value());
            return e;
        });
        entity.setOfflineEnabled(policy.offlineEnabled());
        entity.setBatchSize(policy.batchSize());
        entity.setValidityDurationIso(policy.validityDuration().toString());
        entity.setSyncAcceptedExtensionIso(policy.syncAcceptedExtension().toString());
        entity.setMaxTicketCount(policy.maxTicketCount());
        entity.setMaxTotalAmount(policy.maxTotalAmount().amount());
        entity.setCurrency(policy.maxTotalAmount().currency().value());
        return toDomain(repo.save(entity));
    }

    private static OfflineLimitPolicy toDomain(TenantOfflinePolicyJpaEntity e) {
        return new OfflineLimitPolicy(
            Boolean.TRUE.equals(e.getOfflineEnabled()),
            e.getBatchSize(),
            Duration.parse(e.getValidityDurationIso()),
            Duration.parse(e.getSyncAcceptedExtensionIso()),
            e.getMaxTicketCount(),
            new Money(e.getMaxTotalAmount(), CurrencyCode.of(e.getCurrency()))
        );
    }
}
