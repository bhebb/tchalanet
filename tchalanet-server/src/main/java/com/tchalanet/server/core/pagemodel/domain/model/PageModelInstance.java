package com.tchalanet.server.core.pagemodel.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.core.pagemodel.domain.exception.PageModelNotEditableException;
import com.tchalanet.server.core.pagemodel.domain.exception.PageModelStateException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PageModelInstance {

  private final UUID id;
  private final UUID tenantId;     // tenant owner (RLS scope)
  private final String logicalId;  // ex: public.home, private.dashboard.cashier
  private final String scope;      // public/private (metadata)
  private final String slug;

  private PageModelStatus status;
  private int schemaVersion;
  private JsonNode modelJson;
  private UUID templateId;

  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;

  private Instant publishedAt;
  private Instant archivedAt;
  private Instant deletedAt;

  private PageModelInstance(
      UUID id,
      UUID tenantId,
      String logicalId,
      String scope,
      String slug,
      PageModelStatus status,
      int schemaVersion,
      JsonNode modelJson,
      UUID templateId) {

    this.id = Objects.requireNonNull(id);
    this.tenantId = Objects.requireNonNull(tenantId);
    this.logicalId = Objects.requireNonNull(logicalId);
    this.scope = scope;
    this.slug = slug;
    this.status = Objects.requireNonNull(status);
    this.schemaVersion = schemaVersion;
    this.modelJson = modelJson;
    this.templateId = templateId;
  }

  public static PageModelInstance createDraft(
      UUID id,
      UUID tenantId,
      String logicalId,
      String scope,
      String slug,
      int schemaVersion,
      JsonNode modelJson,
      UUID templateId,
      Instant now,
      UUID actorId) {

    var inst =
        new PageModelInstance(
            id, tenantId, logicalId, scope, slug, PageModelStatus.DRAFT, schemaVersion, modelJson, templateId);

    inst.createdAt = now;
    inst.updatedAt = now;
    inst.createdBy = actorId;
    inst.updatedBy = actorId;
    return inst;
  }

  // Rehydrate from persistence (all fields)
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

    var inst = new PageModelInstance(id, tenantId, logicalId, scope, slug, status, schemaVersion, modelJson, templateId);
    inst.createdAt = createdAt;
    inst.updatedAt = updatedAt;
    inst.createdBy = createdBy;
    inst.updatedBy = updatedBy;
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
      UUID templateId,
      Instant now,
      UUID actorId) {

    if (deletedAt != null) throw new PageModelNotEditableException("Cannot update a deleted PageModel");
    if (status == PageModelStatus.ARCHIVED) throw new PageModelNotEditableException("Cannot update an archived PageModel");

    this.schemaVersion = schemaVersion;
    this.modelJson = modelJson;
    this.templateId = templateId;

    this.updatedAt = now;
    this.updatedBy = actorId;
  }

  public void markPublished(Instant now, UUID actorId) {
    if (deletedAt != null) throw new PageModelStateException("Cannot publish a deleted PageModel");
    if (status == PageModelStatus.ARCHIVED) throw new PageModelStateException("Cannot publish an archived PageModel");

    this.status = PageModelStatus.PUBLISHED;
    this.publishedAt = now;
    this.updatedAt = now;
    this.updatedBy = actorId;
  }

  public void markArchived(Instant now, UUID actorId) {
    if (deletedAt != null) throw new PageModelStateException("Cannot archive a deleted PageModel");
    this.status = PageModelStatus.ARCHIVED;
    this.archivedAt = now;
    this.updatedAt = now;
    this.updatedBy = actorId;
  }

  public void softDelete(Instant now, UUID actorId) {
    this.deletedAt = now;
    this.updatedAt = now;
    this.updatedBy = actorId;
  }

  // getters
  public UUID id() { return id; }
  public UUID tenantId() { return tenantId; }
  public String logicalId() { return logicalId; }
  public String scope() { return scope; }
  public String slug() { return slug; }
  public PageModelStatus status() { return status; }
  public int schemaVersion() { return schemaVersion; }
  public JsonNode modelJson() { return modelJson; }
  public Optional<UUID> templateId() { return Optional.ofNullable(templateId); }

  // audit getters
  public Instant createdAt() { return createdAt; }
  public Instant updatedAt() { return updatedAt; }
  public UUID createdBy() { return createdBy; }
  public UUID updatedBy() { return updatedBy; }
  public Optional<Instant> publishedAt() { return Optional.ofNullable(publishedAt); }
  public Optional<Instant> archivedAt() { return Optional.ofNullable(archivedAt); }
  public Optional<Instant> deletedAt() { return Optional.ofNullable(deletedAt); }
}

