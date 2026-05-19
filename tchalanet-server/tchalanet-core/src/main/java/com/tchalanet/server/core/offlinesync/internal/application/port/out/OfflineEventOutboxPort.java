package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.event.DomainEvent;

/**
 * Records an outbound domain event in the same transaction as the business write. A
 * scheduler later picks it up and publishes it via the in-process event bus, guaranteeing
 * at-least-once delivery across pod restarts.
 */
public interface OfflineEventOutboxPort {

    /** Persist an event for later publication. Called inside the writer transaction. */
    void record(DomainEvent event);
}
