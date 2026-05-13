package com.tchalanet.server.core.terminal.internal.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record Terminal(
    TenantId tenantId,
    TerminalId id,
    OutletId outletId,
    UserId assignedUserId,
    TerminalKind kind,
    TerminalState state,
    boolean autoSessionEnabled,
    TerminalSyncState syncState,
    String code,
    String label,
    String inventoryTag,
    Instant lockedAt,
    UserId lockedBy,
    String lockReason,
    boolean salesBlocked,
    String salesBlockReason,
    Instant salesBlockedAt,
    UserId salesBlockedBy,
    boolean payoutBlocked,
    String payoutBlockReason,
    Instant payoutBlockedAt,
    UserId payoutBlockedBy,
    boolean offlineBlocked,
    String offlineBlockReason,
    Instant offlineBlockedAt,
    UserId offlineBlockedBy,
    Instant lastSeen,
    Map<String, Object> metadata,
    Instant registeredAt,
    Instant unregisteredAt
) {

    public static Terminal createNew(
        TerminalId id,
        TenantId tenantId,
        OutletId outletId,
        TerminalKind kind,
        String label,
        String inventoryTag,
        Map<String, Object> metadata,
        Instant now) {

        return new Terminal(
            tenantId,
            id,
            outletId,
            null,
            kind == null ? TerminalKind.PHYSICAL : kind,
            TerminalState.REGISTERED,
            false,
            TerminalSyncState.ONLINE,
            null,
            label,
            inventoryTag,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            now,
            metadata == null ? Map.of() : Map.copyOf(metadata),
            now,
            null);
    }

    public Terminal register(Instant now) {
        return copyWith(
            state == TerminalState.REGISTERED ? TerminalState.ACTIVE : state,
            autoSessionEnabled,
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
            autoSessionEnabled,
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

    public Terminal unlock() {
        if (state != TerminalState.LOCKED) return this;

        return copyWith(
            TerminalState.ACTIVE,
            autoSessionEnabled,
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

    public boolean locked() {
        return lockedAt != null && lockedBy != null && lockReason != null;
    }

    public Terminal heartbeat(Instant now) {
        return copyWith(
            state,
            autoSessionEnabled,
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

    public Terminal assignToOutlet(OutletId newOutletId) {
        if (newOutletId == null || newOutletId.equals(outletId)) return this;

        return copyWith(
            state,
            autoSessionEnabled,
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
        if (Objects.equals(userId, assignedUserId)) return this;

        return copyWith(
            state,
            userId != null && autoSessionEnabled,
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

    public Terminal enableAutoSession() {
        if (assignedUserId == null) {
            throw new IllegalStateException("Cannot enable auto session: no user assigned");
        }
        if (state != TerminalState.ACTIVE) {
            throw new IllegalStateException("Cannot enable auto session: terminal is not active");
        }
        if (autoSessionEnabled) return this;

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

    public Terminal disableAutoSession() {
        if (!autoSessionEnabled) return this;

        return copyWith(
            state,
            false,
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

    public boolean isAutoSessionEligible() {
        return state == TerminalState.ACTIVE && assignedUserId != null && autoSessionEnabled;
    }

    public Terminal updateSyncState(TerminalSyncState newSyncState, Instant now) {
        if (newSyncState == syncState) return this;

        return copyWith(
            state,
            autoSessionEnabled,
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

    public Terminal mergeMetadata(Map<String, Object> patch) {
        if (patch == null || patch.isEmpty()) return this;

        var merged = new HashMap<>(metadata == null ? Map.<String, Object>of() : metadata);
        merged.putAll(patch);

        return copyWith(
            state,
            autoSessionEnabled,
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

    private Terminal copyWith(
        TerminalState newState,
        boolean newAutoSessionEnabled,
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
            tenantId,
            id,
            newOutletId,
            newAssignedUserId,
            kind,
            newState,
            newAutoSessionEnabled,
            newSyncState,
            code,
            newLabel,
            newInventoryTag,
            newLockedAt,
            newLockedBy,
            newLockReason,
            salesBlocked,
            salesBlockReason,
            salesBlockedAt,
            salesBlockedBy,
            payoutBlocked,
            payoutBlockReason,
            payoutBlockedAt,
            payoutBlockedBy,
            offlineBlocked,
            offlineBlockReason,
            offlineBlockedAt,
            offlineBlockedBy,
            newLastSeen,
            newMetadata == null ? Map.of() : Map.copyOf(newMetadata),
            newRegisteredAt,
            newUnregisteredAt);
    }

    public boolean assignedTo(UserId userId) {
        return assignedUserId == null || assignedUserId.equals(userId);
    }

    public String displayCode() {
        if (code != null && !code.isBlank()) return code;
        if (inventoryTag != null && !inventoryTag.isBlank()) return inventoryTag;
        return label;
    }

    public boolean salesAllowed() {
        return !locked() && !salesBlocked;
    }

    public boolean payoutAllowed() {
        return !locked() && !payoutBlocked;
    }

    public boolean offlineAllowedForGrant() {
        return !locked() && !salesBlocked && !offlineBlocked;
    }

    public boolean canReceiveOfflineSyncForAudit() {
        return !locked();
    }
}
