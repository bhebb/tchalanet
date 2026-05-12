package com.tchalanet.server.catalog.pagemodeltemplate.api.event;

import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Application event emitted when a page model template changes.
 *
 * This is not a business DomainEvent: catalog publishes it so downstream projections or drafts can
 * refresh after commit without making catalog depend on core.
 */
public record PageModelTemplateUpdatedEvent(
    PageModelTemplateId templateId,
    String logicalId,
    JsonNode newModel,
    int newSchemaVersion,
    UserId actorId,
    Instant occurredAt) {}
