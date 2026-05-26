package com.tchalanet.server.core.terminal.internal.domain.model.binding;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalSurface;
import java.time.Instant;
import java.util.Objects;

public record TerminalDeviceBinding(
    TerminalBindingId id,
    TenantId tenantId,
    TerminalId terminalId,
    TerminalBindingType bindingType,
    TerminalBindingStatus status,
    String bindingPublicKey,
    String bindingSecretHash,
    String deviceFingerprintHash,
    Instant boundAt,
    Instant expiresAt,
    Instant revokedAt,
    Instant lastSeenAt
) {

    public TerminalDeviceBinding {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(terminalId, "terminalId is required");
        Objects.requireNonNull(bindingType, "bindingType is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(boundAt, "boundAt is required");
        if (status == TerminalBindingStatus.REVOKED && revokedAt == null) {
            throw new IllegalArgumentException("revokedAt is required for revoked binding");
        }
    }

    public static TerminalDeviceBinding active(
        TerminalBindingId id,
        TenantId tenantId,
        TerminalId terminalId,
        TerminalBindingType bindingType,
        String bindingPublicKey,
        String bindingSecretHash,
        String deviceFingerprintHash,
        Instant boundAt,
        Instant expiresAt
    ) {
        return new TerminalDeviceBinding(
            id,
            tenantId,
            terminalId,
            bindingType,
            TerminalBindingStatus.ACTIVE,
            bindingPublicKey,
            bindingSecretHash,
            deviceFingerprintHash,
            boundAt,
            expiresAt,
            null,
            null
        );
    }

    public boolean compatibleWith(TerminalKind kind, TerminalSurface surface) {
        return switch (bindingType) {
            case POS_DEVICE -> kind == TerminalKind.PHYSICAL && surface == TerminalSurface.POS;
            case MOBILE_APP -> kind == TerminalKind.VIRTUAL && surface == TerminalSurface.MOBILE;
            case ADMIN_SELECTION -> kind == TerminalKind.VIRTUAL
                && (surface == TerminalSurface.WEB || surface == TerminalSurface.BACK_OFFICE);
        };
    }

    public boolean activeAt(Instant now) {
        return status == TerminalBindingStatus.ACTIVE && (expiresAt == null || expiresAt.isAfter(now));
    }

    public TerminalDeviceBinding expireIfDue(Instant now) {
        if (status != TerminalBindingStatus.ACTIVE || expiresAt == null || expiresAt.isAfter(now)) {
            return this;
        }
        return new TerminalDeviceBinding(
            id,
            tenantId,
            terminalId,
            bindingType,
            TerminalBindingStatus.EXPIRED,
            bindingPublicKey,
            bindingSecretHash,
            deviceFingerprintHash,
            boundAt,
            expiresAt,
            revokedAt,
            lastSeenAt
        );
    }

    public TerminalDeviceBinding revoke(Instant now) {
        if (status == TerminalBindingStatus.REVOKED) {
            return this;
        }
        return new TerminalDeviceBinding(
            id,
            tenantId,
            terminalId,
            bindingType,
            TerminalBindingStatus.REVOKED,
            bindingPublicKey,
            bindingSecretHash,
            deviceFingerprintHash,
            boundAt,
            expiresAt,
            Objects.requireNonNull(now, "now is required"),
            lastSeenAt
        );
    }
}
