package com.tchalanet.server.core.reconciliation.internal.infra.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.TenantId;

@Getter
public class ReconciliationRunCompletedEvent extends ApplicationEvent {

    private final ReconciliationRunId runId;
    private final TenantId tenantId;
    private final LocalDate businessDate;

    public ReconciliationRunCompletedEvent(Object source, ReconciliationRunId runId, TenantId tenantId, LocalDate businessDate) {
        super(source);
        this.runId = runId;
        this.tenantId = tenantId;
        this.businessDate = businessDate;
    }
}
