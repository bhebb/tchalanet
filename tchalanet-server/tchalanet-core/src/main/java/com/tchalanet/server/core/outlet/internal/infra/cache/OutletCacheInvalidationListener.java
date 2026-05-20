package com.tchalanet.server.core.outlet.internal.infra.cache;

import com.tchalanet.server.core.outlet.internal.domain.event.OutletConfigUpdatedEvent;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletCreatedEvent;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletDayClosedEvent;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletDayReopenedEvent;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletSalesBlockedEvent;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletSalesUnblockedEvent;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletUserAssignedEvent;
import com.tchalanet.server.core.outlet.internal.domain.event.OutletUserRemovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutletCacheInvalidationListener {

    private final OutletCacheEvictor evictor;

    @EventListener
    public void onOutletCreated(OutletCreatedEvent event) {
        evictor.evictOutletLists();
    }

    @EventListener
    public void onOutletConfigUpdated(OutletConfigUpdatedEvent event) {
        evictor.evictOutlet(event.outletId());
    }

    @EventListener
    public void onOutletSalesBlocked(OutletSalesBlockedEvent event) {
        evictor.evictSalesCapability(event.outletId());
    }

    @EventListener
    public void onOutletSalesUnblocked(OutletSalesUnblockedEvent event) {
        evictor.evictSalesCapability(event.outletId());
    }

    @EventListener
    public void onOutletDayClosed(OutletDayClosedEvent event) {
        evictor.evictSalesCapability(event.outletId());
    }

    @EventListener
    public void onOutletDayReopened(OutletDayReopenedEvent event) {
        evictor.evictSalesCapability(event.outletId());
    }

    @EventListener
    public void onOutletUserAssigned(OutletUserAssignedEvent event) {
        evictor.evictOutletRelations(event.outletId());
    }

    @EventListener
    public void onOutletUserRemoved(OutletUserRemovedEvent event) {
        evictor.evictOutletRelations(event.outletId());
    }
}
