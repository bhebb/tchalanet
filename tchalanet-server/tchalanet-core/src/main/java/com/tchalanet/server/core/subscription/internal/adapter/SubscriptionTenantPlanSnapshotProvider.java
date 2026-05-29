package com.tchalanet.server.core.subscription.internal.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.subscription.internal.application.port.out.SubscriptionReaderPort;
import com.tchalanet.server.core.subscription.internal.domain.model.SubscriptionStatus;
import com.tchalanet.server.platform.entitlement.api.TenantPlanSnapshotProvider;
import com.tchalanet.server.platform.entitlement.api.model.TenantPlanSnapshot;
import com.tchalanet.server.platform.entitlement.api.model.TenantPlanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SubscriptionTenantPlanSnapshotProvider
    implements TenantPlanSnapshotProvider {

    private final SubscriptionReaderPort reader;

    @Override
    public Optional<TenantPlanSnapshot> findCurrentPlan(TenantId tenantId) {
        return reader.findByTenantId(tenantId)
            .map(sub -> new TenantPlanSnapshot(
                sub.tenantId(),
                sub.planCode(),
                mapStatus(sub.status()),
                sub.startedAt(),
                sub.endsAt()
            ));
    }

    private TenantPlanStatus mapStatus(SubscriptionStatus status) {
        return switch (status) {
            case TRIAL -> TenantPlanStatus.TRIAL;
            case ACTIVE -> TenantPlanStatus.ACTIVE;
            case SUSPENDED -> TenantPlanStatus.SUSPENDED;
            case CANCELED -> TenantPlanStatus.CANCELED;
            case EXPIRED -> TenantPlanStatus.EXPIRED;
        };
    }
}
