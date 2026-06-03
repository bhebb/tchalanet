package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Set;

/**
 * Domain event emitted when tenant identity fields change.
 *
 * <p>Expected future consumers include cache invalidation and search indexing.
 */
public record TenantIdentityUpdatedEvent(
    EventId eventId, Instant occurredAt, TenantId tenantId, Set<String> changedFields)
    implements DomainEvent {}
