package com.tchalanet.server.core.terminal.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.Map;

/**
 * Terminal aggregate root. Immutable record. State transitions return new instances.
 *
 * <p>Conventions: typed IDs only (no raw UUID), no Lombok mutators, factory + business methods,
 * domain enums aligned with V100 CHECK constraints.
 */
public record Terminal(
    TerminalId id,
    TenantId tenantId,
    OutletId outletId,
    UserId assignedUserId,
    TerminalKind kind,
    TerminalState state,
    boolean activeForUser,
    TerminalSyncState syncState,
    Instant lastSeen,
    String label,
    String inventoryTag,
    Map<String, Object> metadata,
    Instant registeredAt,
    Instant unregisteredAt,
    Instant lockedAt,
    UserId lockedBy,
    String lockReason) {

  // ── Factories ──────────────────────────────────────────────────────────

  public static Terminal createNew(
      TerminalId id,
      TenantId tenantId,
      OutletId outletId,
      TerminalKind kind,
      String label,
      String inventoryTag,
      Map<String, Object> metadata) {
    return new Terminal(
        id,
        tenantId,
        outletId,
        null,
        kind == null ? TerminalKind.PHYSICAL : kind,
        TerminalState.REGISTERED,
        false,
        TerminalSyncState.ONLINE,
        null,
        label,
        inventoryTag,
        metadata == null ? Map.of() : Map.copyOf(metadata),
        Instant.now(),
        null,
        null,
        null,
        null);
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────

  public Terminal register(Instant now) {
    return copyWith(
        state == TerminalState.REGISTERED ? TerminalState.ACTIVE : state,
        activeForUser,
        syncState,
        now,
        registeredAt == null ? now : registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        outletId,
        assignedUserId,
        label,
        inventoryTag,
        metadata);
  }

  public Terminal unregister(UserId by, Instant now) {
    return copyWith(
        TerminalState.UNREGISTERED,
        false,
        syncState,
        lastSeen,
        registeredAt,
        now,
        lockedAt,
        lockedBy,
        lockReason,
        outletId,
        assignedUserId,
        label,
        inventoryTag,
        metadata);
  }

  public Terminal lock(UserId by, String reason, Instant now) {
    return copyWith(
        TerminalState.LOCKED,
        activeForUser,
        syncState,
        lastSeen,
        registeredAt,
        unregisteredAt,
        now,
        by,
        reason,
        outletId,
        assignedUserId,
        label,
        inventoryTag,
        metadata);
  }

  public Terminal unlock(Instant now) {
    if (state != TerminalState.LOCKED) return this;
    return copyWith(
        TerminalState.ACTIVE,
        activeForUser,
        syncState,
        lastSeen,
        registeredAt,
        unregisteredAt,
        null,
        null,
        null,
        outletId,
        assignedUserId,
        label,
        inventoryTag,
        metadata);
  }

  public Terminal heartbeat(Instant now) {
    return copyWith(
        state,
        activeForUser,
        TerminalSyncState.ONLINE,
        now,
        registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        outletId,
        assignedUserId,
        label,
        inventoryTag,
        metadata);
  }

  // ── Assignment ─────────────────────────────────────────────────────────

  public Terminal assignToOutlet(OutletId newOutletId) {
    if (newOutletId == null || newOutletId.equals(this.outletId)) return this;
    return copyWith(
        state,
        activeForUser,
        syncState,
        lastSeen,
        registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        newOutletId,
        assignedUserId,
        label,
        inventoryTag,
        metadata);
  }

  public Terminal assignToUser(UserId userId) {
    if (java.util.Objects.equals(userId, this.assignedUserId)) return this;
    return copyWith(
        state,
        userId == null ? false : activeForUser,
        syncState,
        lastSeen,
        registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        outletId,
        userId,
        label,
        inventoryTag,
        metadata);
  }

  public Terminal activateForUser() {
    if (assignedUserId == null) {
      throw new IllegalStateException("Cannot activate terminal: no user assigned");
    }
    if (activeForUser) return this;
    return copyWith(
        state,
        true,
        syncState,
        lastSeen,
        registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        outletId,
        assignedUserId,
        label,
        inventoryTag,
        metadata);
  }

  // ── Sync ──────────────────────────────────────────────────────────────

  public Terminal updateSyncState(TerminalSyncState newSyncState, Instant now) {
    if (newSyncState == this.syncState) return this;
    return copyWith(
        state,
        activeForUser,
        newSyncState,
        now,
        registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        outletId,
        assignedUserId,
        label,
        inventoryTag,
        metadata);
  }

  // ── Metadata ──────────────────────────────────────────────────────────

  public Terminal mergeMetadata(Map<String, Object> patch) {
    if (patch == null || patch.isEmpty()) return this;
    java.util.Map<String, Object> merged = new java.util.HashMap<>(metadata);
    merged.putAll(patch);
    return copyWith(
        state,
        activeForUser,
        syncState,
        lastSeen,
        registeredAt,
        unregisteredAt,
        lockedAt,
        lockedBy,
        lockReason,
        outletId,
        assignedUserId,
        label,
        inventoryTag,
        Map.copyOf(merged));
  }

  // ── Internal helper to keep copy-pattern compact ──────────────────────

  private Terminal copyWith(
      TerminalState newState,
      boolean newActiveForUser,
      TerminalSyncState newSyncState,
      Instant newLastSeen,
      Instant newRegisteredAt,
      Instant newUnregisteredAt,
      Instant newLockedAt,
      UserId newLockedBy,
      String newLockReason,
      OutletId newOutletId,
      UserId newAssignedUserId,
      String newLabel,
      String newInventoryTag,
      Map<String, Object> newMetadata) {
    return new Terminal(
        id,
        tenantId,
        newOutletId,
        newAssignedUserId,
        kind,
        newState,
        newActiveForUser,
        newSyncState,
        newLastSeen,
        newLabel,
        newInventoryTag,
        newMetadata,
        newRegisteredAt,
        newUnregisteredAt,
        newLockedAt,
        newLockedBy,
        newLockReason);
  }
}
