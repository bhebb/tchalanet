package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.core.user.domain.model.AppUser;

final class UserMapper {

    private UserMapper() {}

    static AppUser toDomain(AppUserJpaEntity e) {
        return AppUser.restore(
            e.getId(),
            e.getKeycloakId(),
            e.getTenantId(),
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
            e.getStatus(),
            e.getApprovedAt(),
            e.getApprovedBy(),
            e.getLastLoginAt(),
            e.getVersion()
        );
    }

    static AppUserJpaEntity toNewEntity(AppUser u) {
        AppUserJpaEntity e = new AppUserJpaEntity();
        e.setId(u.id());
        apply(e, u);
        return e;
    }

    static AppUserJpaEntity merge(AppUserJpaEntity e, AppUser u) {
        apply(e, u);
        return e;
    }

    private static void apply(AppUserJpaEntity e, AppUser u) {
        e.setKeycloakId(u.keycloakId());
        e.setTenantId(u.tenantId());
        e.setTenantCode(u.tenantCode());
        e.setUsername(u.username());
        e.setEmail(u.email());
        e.setPhone(u.phone());
        e.setFirstName(u.firstName());
        e.setLastName(u.lastName());
        e.setDisplayName(u.displayName());
        e.setAvatarUrl(u.avatarUrl());
        e.setLocale(u.locale());
        e.setTimeZone(u.timeZone());
        e.setStatus(u.status());
        e.setApprovedAt(u.approvedAt());
        e.setApprovedBy(u.approvedBy());
        e.setLastLoginAt(u.lastLoginAt());
        e.setVersion(u.version());
    }
}
