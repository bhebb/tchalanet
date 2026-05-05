package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.exception.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public final class Draw {

    private final DrawId id;
    private final TenantId tenantId;
    private final DrawChannelId drawChannelId;

    private LocalDate drawDate;
    private Instant scheduledAt;
    private Instant cutoffAt;

    private DrawStatus status;
    private DrawResultId drawResultId;

    private Instant openedAt;
    private Instant closedAt;
    private Instant resultedAt;
    private Instant settledAt;
    private Instant canceledAt;
    private String cancelReason;

    private DrawSource resultSource;
    private String resultOverrideReason;
    private Instant resultOverriddenAt;

    private boolean locked;
    private boolean systemGenerated;

    public Draw(
        DrawId id,
        TenantId tenantId,
        DrawChannelId drawChannelId,
        LocalDate drawDate,
        Instant scheduledAt,
        Instant cutoffAt,
        DrawStatus status,
        DrawResultId drawResultId,
        Instant openedAt,
        Instant closedAt,
        Instant resultedAt,
        Instant settledAt,
        Instant canceledAt,
        String cancelReason,
        DrawSource resultSource,
        String resultOverrideReason,
        Instant resultOverriddenAt,
        boolean locked,
        boolean systemGenerated) {

        this.id = Objects.requireNonNull(id, "id is required");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId is required");
        this.drawChannelId = Objects.requireNonNull(drawChannelId, "drawChannelId is required");
        this.drawDate = Objects.requireNonNull(drawDate, "drawDate is required");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt is required");
        this.cutoffAt = Objects.requireNonNull(cutoffAt, "cutoffAt is required");
        requireValidSchedule(scheduledAt, cutoffAt);
        this.status = Objects.requireNonNull(status, "status is required");
        this.drawResultId = drawResultId;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.resultedAt = resultedAt;
        this.settledAt = settledAt;
        this.canceledAt = canceledAt;
        this.cancelReason = cancelReason;
        this.resultSource = resultSource;
        this.resultOverrideReason = resultOverrideReason;
        this.resultOverriddenAt = resultOverriddenAt;
        this.locked = locked;
        this.systemGenerated = systemGenerated;
    }

    public static Draw scheduled(
        DrawId id,
        TenantId tenantId,
        DrawChannelId drawChannelId,
        LocalDate drawDate,
        Instant scheduledAt,
        Instant cutoffAt) {
        return new Draw(
            id,
            tenantId,
            drawChannelId,
            drawDate,
            scheduledAt,
            cutoffAt,
            DrawStatus.SCHEDULED,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            true);
    }

    public DrawId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public DrawChannelId drawChannelId() {
        return drawChannelId;
    }

    public LocalDate drawDate() {
        return drawDate;
    }

    public Instant scheduledAt() {
        return scheduledAt;
    }

    public Instant cutoffAt() {
        return cutoffAt;
    }

    public DrawStatus status() {
        return status;
    }

    public DrawResultId drawResultId() {
        return drawResultId;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public Instant resultedAt() {
        return resultedAt;
    }

    public Instant settledAt() {
        return settledAt;
    }

    public Instant canceledAt() {
        return canceledAt;
    }

    public String cancelReason() {
        return cancelReason;
    }

    public DrawSource resultSource() {
        return resultSource;
    }

    public String resultOverrideReason() {
        return resultOverrideReason;
    }

    public Instant resultOverriddenAt() {
        return resultOverriddenAt;
    }

    public boolean locked() {
        return locked;
    }

    public boolean systemGenerated() {
        return systemGenerated;
    }

    public void open(Instant now) {
        ensureNotLocked();
        DrawStatusTransition.check(this.status, DrawStatus.OPEN);
        this.status = DrawStatus.OPEN;
        this.openedAt = Objects.requireNonNull(now, "now is required");
    }

    public void close(Instant now) {
        ensureNotLocked();
        DrawStatusTransition.check(this.status, DrawStatus.CLOSED);
        this.status = DrawStatus.CLOSED;
        this.closedAt = Objects.requireNonNull(now, "now is required");
    }

    public void applyResult(DrawResultId resultId, Instant now, DrawSource resultSource) {
        ensureNotLocked();
        DrawStatusTransition.check(this.status, DrawStatus.RESULTED);

        if (this.drawResultId != null) {
            throw new DrawInvalidResultException(id, "Draw already has a result");
        }

        this.drawResultId = Objects.requireNonNull(resultId, "resultId is required");
        this.status = DrawStatus.RESULTED;
        this.resultedAt = Objects.requireNonNull(now, "now is required");
        this.resultSource = Objects.requireNonNull(resultSource, "resultSource is required");
    }

    public void overrideResult(DrawResultId resultId, Instant now, String reason) {
        ensureNotLocked();

        if (this.status != DrawStatus.RESULTED) {
            throw new DrawInvalidOverrideException(
                id,
                "Can only override result for RESULTED draws (current: " + this.status + ")");
        }

        if (reason == null || reason.isBlank()) {
            throw new DrawInvalidOverrideException(id, "Override reason is required");
        }

        this.drawResultId = Objects.requireNonNull(resultId, "resultId is required");
        this.resultSource = DrawSource.ADMIN_OVERRIDE;
        this.resultOverrideReason = reason.trim();
        this.resultOverriddenAt = Objects.requireNonNull(now, "now is required");
    }

    public void settle(Instant now) {
        ensureNotLocked();
        DrawStatusTransition.check(this.status, DrawStatus.SETTLED);

        if (this.drawResultId == null) {
            throw new DrawCannotSettleWithoutResultException(id);
        }

        this.status = DrawStatus.SETTLED;
        this.settledAt = Objects.requireNonNull(now, "now is required");
    }

    public void cancel(String reason, Instant now) {
        ensureNotLocked();
        DrawStatusTransition.check(this.status, DrawStatus.CANCELED);

        if (reason == null || reason.isBlank()) {
            throw new DrawInvalidCancelException(id, "Cancel reason is required");
        }

        this.cancelReason = reason.trim();
        this.canceledAt = Objects.requireNonNull(now, "now is required");
        this.status = DrawStatus.CANCELED;
    }

    public void reschedule(LocalDate drawDate, Instant scheduledAt, Instant cutoffAt) {
        ensureNotLocked();

        if (this.status != DrawStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot reschedule draw that is not SCHEDULED (current: " + this.status + ")");
        }

        this.drawDate = Objects.requireNonNull(drawDate, "drawDate is required");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt is required");
        this.cutoffAt = Objects.requireNonNull(cutoffAt, "cutoffAt is required");
        requireValidSchedule(scheduledAt, cutoffAt);
    }

    public void archive() {
        ensureNotLocked();
        DrawStatusTransition.check(this.status, DrawStatus.ARCHIVED);
        this.status = DrawStatus.ARCHIVED;
    }

    public void lock() {
        if (this.locked) {
            return;
        }
        this.locked = true;
    }

    public void unlock() {
        if (!this.locked) {
            return;
        }
        this.locked = false;
    }

    public void markSystemGenerated(boolean systemGenerated) {
        this.systemGenerated = systemGenerated;
    }

    private static void requireValidSchedule(Instant scheduledAt, Instant cutoffAt) {
        Objects.requireNonNull(scheduledAt, "scheduledAt is required");
        Objects.requireNonNull(cutoffAt, "cutoffAt is required");

        if (!cutoffAt.isBefore(scheduledAt)) {
            throw new IllegalArgumentException("cutoffAt must be before scheduledAt");
        }
    }

    private void ensureNotLocked() {
        if (locked) {
            throw new DrawLockedException(id);
        }
    }
}
