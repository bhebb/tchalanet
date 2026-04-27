package com.tchalanet.server.core.pos.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Terminal {

  private final UUID id;
  private final TenantId tenantId;
  private OutletId outletId;
  private TerminalState state;
  private Instant lastSeen;
  private String meta;
  private long version;
  private Instant registeredAt;
  private Instant unregisteredAt;
  private Instant lockedAt;
  private UUID lockedBy;
  private String lockReason;
  private Instant deletedAt;

  // New fields
  private String label;
  private String inventoryTag;

  public Terminal(
      UUID id,
      TenantId tenantId,
      OutletId outletId,
      TerminalState state,
      Instant lastSeen,
      String meta,
      long version,
      Instant registeredAt,
      Instant unregisteredAt,
      Instant lockedAt,
      UUID lockedBy,
      String lockReason,
      Instant deletedAt,
      String label,
      String inventoryTag) {
    this.id = id;
    this.tenantId = tenantId;
    this.outletId = outletId;
    this.state = state;
    this.lastSeen = lastSeen;
    this.meta = meta;
    this.version = version;
    this.registeredAt = registeredAt;
    this.unregisteredAt = unregisteredAt;
    this.lockedAt = lockedAt;
    this.lockedBy = lockedBy;
    this.lockReason = lockReason;
    this.deletedAt = deletedAt;
    this.label = label;
    this.inventoryTag = inventoryTag;
  }

  // Backward-compatible constructor (pre-label fields)
  public Terminal(
      UUID id,
      TenantId tenantId,
      OutletId outletId,
      TerminalState state,
      Instant lastSeen,
      String meta,
      long version,
      Instant registeredAt,
      Instant unregisteredAt,
      Instant lockedAt,
      UUID lockedBy,
      String lockReason,
      Instant deletedAt) {
    this(
        id,
        tenantId,
        outletId,
        state,
        lastSeen,
        meta,
        version,
        registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        deletedAt,
        null,
        null);
  }

  // Behaviors
  public void register(Instant now) {
    this.state = TerminalState.ACTIVE;
    this.lastSeen = now;
  }

  public void heartbeat(Instant now, String metaDelta) {
    this.lastSeen = now;
    if (metaDelta != null) {
      this.meta = metaDelta; // TODO: merge
    }
  }

  public void lock(Instant now, String reason) {
    this.state = TerminalState.BLOCKED;
    // Optionally store reason in meta
  }

  public void unlock(Instant now) {
    this.state = TerminalState.ACTIVE;
  }

  // New methods
  public Terminal lock(UUID by, String reason, Instant now) {
    return new Terminal(
        id,
        tenantId,
        outletId,
        TerminalState.BLOCKED,
        lastSeen,
        meta,
        version,
        registeredAt,
        unregisteredAt,
        now,
        by,
        reason,
        deletedAt,
        label,
        inventoryTag);
  }

  public Terminal unlock(UUID by, Instant now) {
    return new Terminal(
        id,
        tenantId,
        outletId,
        TerminalState.ACTIVE,
        lastSeen,
        meta,
        version,
        registeredAt,
        unregisteredAt,
        null,
        null,
        null,
        deletedAt,
        label,
        inventoryTag);
  }

  public Terminal unregister(UUID by, Instant now) {
    return new Terminal(
        id,
        tenantId,
        outletId,
        state,
        lastSeen,
        meta,
        version,
        registeredAt,
        now,
        lockedAt,
        lockedBy,
        lockReason,
        now,
        label,
        inventoryTag);
  }

  public Terminal mergeMetadata(Map<String, Object> patch, Instant now) {
    // TODO: implement proper merge
    String newMeta = meta; // placeholder
    return new Terminal(
        id,
        tenantId,
        outletId,
        state,
        lastSeen,
        newMeta,
        version,
        registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        deletedAt,
        label,
        inventoryTag);
  }

  // Getters
  public UUID id() {
    return id;
  }

  public TenantId tenantId() {
    return tenantId;
  }

  public OutletId outletId() {
    return outletId;
  }

  public TerminalState state() {
    return state;
  }

  public Instant lastSeen() {
    return lastSeen;
  }

  public String meta() {
    return meta;
  }

  public long version() {
    return version;
  }

  public Instant registeredAt() {
    return registeredAt;
  }

  public Instant unregisteredAt() {
    return unregisteredAt;
  }

  public Instant lockedAt() {
    return lockedAt;
  }

  public UUID lockedBy() {
    return lockedBy;
  }

  public String lockReason() {
    return lockReason;
  }

  public Instant deletedAt() {
    return deletedAt;
  }

  public String label() {
    return label;
  }

  public String inventoryTag() {
    return inventoryTag;
  }

  public enum TerminalState {
    ACTIVE,
    INACTIVE,
    BLOCKED
  }
}
