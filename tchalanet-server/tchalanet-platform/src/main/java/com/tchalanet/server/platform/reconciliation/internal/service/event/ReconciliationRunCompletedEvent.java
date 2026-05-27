package com.tchalanet.server.platform.reconciliation.internal.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class ReconciliationRunCompletedEvent extends ApplicationEvent {

    private final UUID runId;
    private final UUID tenantId;
    private final LocalDate businessDate;

    public ReconciliationRunCompletedEvent(Object source, UUID runId, UUID tenantId, LocalDate businessDate) {
        super(source);
        this.runId = runId;
        this.tenantId = tenantId;
        this.businessDate = businessDate;
    }
}

