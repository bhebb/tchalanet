package com.tchalanet.server.core.user.infra.persistence;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.UserStatus;

import com.tchalanet.server.core.user.domain.model.AppUser;

final class UserMapper {

    private UserMapper() {}

    static AppUser toDomain(AppUserJpaEntity e) {
        return AppUser.restore(
            UserId.of(e.getId()),
            e.getKeycloakId(),
            TenantId.of(e.getTenantId()),
            e.getTenantCode(),
            e.getUsername(),
            e.getEmail(),
            e.getPhone(),
            e.getFirstName(),
            e.getLastName(),
            e.getDisplayName(),
            e.getAvatarUrl(),
            e.getLocale(),
            e.getTimeZone(),
            UserStatus.valueOf(e.getStatus().name()),
            e.getApprovedAt(),
            e.getApprovedBy(),
            e.getLastLoginAt(),
            e.getVersion()
        );
    }

    static AppUserJpaEntity toNewEntity(AppUser u) {
        AppUserJpaEntity e = new AppUserJpaEntity();
        if (u.getId() != null) e.setId(u.getId().uuid());
        apply(e, u);
        return e;
    }

    static AppUserJpaEntity merge(AppUserJpaEntity e, AppUser u) {
        apply(e, u);
        return e;
    }

    private static void apply(AppUserJpaEntity e, AppUser u) {
        e.setKeycloakId(u.getKeycloakId());
        if (u.getTenantId() != null) e.setTenantId(u.getTenantId().uuid());
        e.setTenantCode(u.getTenantCode());
        e.setUsername(u.getUsername());
        e.setEmail(u.getEmail());
        e.setPhone(u.getPhone());
        e.setFirstName(u.getFirstName());
        e.setLastName(u.getLastName());
        e.setDisplayName(u.getDisplayName());
        e.setAvatarUrl(u.getAvatarUrl());
        e.setLocale(u.getLocale());
        e.setTimeZone(u.getTimeZone());
        e.setStatus(com.tchalanet.server.common.types.enums.UserStatus.valueOf(u.getStatus().name()));
        e.setApprovedAt(u.getApprovedAt());
        e.setApprovedBy(u.getApprovedBy());
        e.setLastLoginAt(u.getLastLoginAt());
        e.setVersion(u.getVersion());
    }
}
