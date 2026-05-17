package com.tchalanet.server.core.pagemodel.internal.domain.model;

import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.internal.domain.exception.PageModelNotEditableException;
import com.tchalanet.server.core.pagemodel.internal.domain.exception.PageModelStateException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PageModelInstance {

    private final PageModelId id;
    private final TenantId tenantId;
    private final String logicalId;
    private final String scope;
    private final String slug;

    private PageModelStatus status;
    private int schemaVersion;
    private JsonNode modelJson;
    private PageModelTemplateId templateId;

    private Instant createdAt;
    private Instant updatedAt;
    private UserId createdBy;
    private UserId updatedBy;

    private Instant publishedAt;
    private Instant archivedAt;
    private Instant deletedAt;

    //todo add value
    private PageModelInstance(
        PageModelId id,
        TenantId tenantId,
        String logicalId,
        String scope,
        String slug,
        PageModelStatus status,
        int schemaVersion,
        JsonNode modelJson,
        PageModelTemplateId templateId) {

        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.logicalId = Objects.requireNonNull(logicalId, "logicalId");
        this.scope = scope;
        this.slug = slug;
        this.status = Objects.requireNonNull(status, "status");
        this.schemaVersion = schemaVersion;
        this.modelJson = Objects.requireNonNull(modelJson, "modelJson");
        this.templateId = templateId;
    }

    public static PageModelInstance createDraft(
        PageModelId id,
        TenantId tenantId,
        String logicalId,
        String scope,
        String slug,
        int schemaVersion,
        JsonNode modelJson,
        PageModelTemplateId templateId,
        Instant now,
        UserId actorId) {

        var inst = new PageModelInstance(
            id, tenantId, logicalId, scope, slug,
            PageModelStatus.DRAFT, schemaVersion, modelJson, templateId);

        inst.createdAt = now;
        inst.updatedAt = now;
        inst.createdBy = actorId;
        inst.updatedBy = actorId;
        return inst;
    }

    public static PageModelInstance rehydrate(
        UUID id,
        UUID tenantId,
        String logicalId,
        String scope,
        String slug,
        PageModelStatus status,
        int schemaVersion,
        JsonNode modelJson,
        UUID templateId,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        Instant publishedAt,
        Instant archivedAt,
        Instant deletedAt) {

        var inst = new PageModelInstance(
            PageModelId.of(id),
            TenantId.of(tenantId),
            logicalId,
            scope,
            slug,
            status,
            schemaVersion,
            modelJson,
            PageModelTemplateId.nullableOf(templateId));

        inst.createdAt = createdAt;
        inst.updatedAt = updatedAt;
        inst.createdBy = UserId.nullableOf(createdBy);
        inst.updatedBy = UserId.nullableOf(updatedBy);
        inst.publishedAt = publishedAt;
        inst.archivedAt = archivedAt;
        inst.deletedAt = deletedAt;
        return inst;
    }

    public void applyUpsert(
        String scope,
        String slug,
        int schemaVersion,
        JsonNode modelJson,
        PageModelTemplateId templateId,
        Instant now,
        UserId actorId) {

        ensureEditable();

        this.schemaVersion = schemaVersion;
        this.modelJson = Objects.requireNonNull(modelJson, "modelJson");
        this.templateId = templateId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markPublished(Instant now, UserId actorId) {
        if (deletedAt != null) {
            throw new PageModelStateException("Cannot publish a deleted PageModel");
        }
        if (status == PageModelStatus.ARCHIVED) {
            throw new PageModelStateException("Cannot publish an archived PageModel");
        }

        this.status = PageModelStatus.PUBLISHED;
        this.publishedAt = now;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markArchived(Instant now, UserId actorId) {
        if (deletedAt != null) {
            throw new PageModelStateException("Cannot archive a deleted PageModel");
        }

        this.status = PageModelStatus.ARCHIVED;
        this.archivedAt = now;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public PageModelInstance resetToTemplate(
        JsonNode templateModel,
        int schemaVersion,
        Instant now,
        UserId actorId) {

        ensureEditable();

        this.modelJson = Objects.requireNonNull(templateModel, "templateModel");
        this.schemaVersion = schemaVersion;
        this.status = PageModelStatus.DRAFT;
        this.publishedAt = null;
        this.updatedAt = now;
        this.updatedBy = actorId;
        return this;
    }

    public void softDelete(Instant now, UserId actorId) {
        this.deletedAt = now;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    private void ensureEditable() {
        if (deletedAt != null) {
            throw new PageModelNotEditableException("Cannot update a deleted PageModel");
        }
        if (status == PageModelStatus.ARCHIVED) {
            throw new PageModelNotEditableException("Cannot update an archived PageModel");
        }
    }

    public PageModelId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public String logicalId() {
        return logicalId;
    }

    public String scope() {
        return scope;
    }

    public String slug() {
        return slug;
    }

    public PageModelStatus status() {
        return status;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public JsonNode modelJson() {
        return modelJson;
    }

    public Optional<PageModelTemplateId> templateId() {
        return Optional.ofNullable(templateId);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public UserId createdBy() {
        return createdBy;
    }

    public UserId updatedBy() {
        return updatedBy;
    }

    public Optional<Instant> publishedAt() {
        return Optional.ofNullable(publishedAt);
    }

    public Optional<Instant> archivedAt() {
        return Optional.ofNullable(archivedAt);
    }

    public Optional<Instant> deletedAt() {
        return Optional.ofNullable(deletedAt);
    }
}
