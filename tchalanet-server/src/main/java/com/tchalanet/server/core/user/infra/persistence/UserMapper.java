package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.core.user.domain.model.AppUser;

final class UserMapper {

  private UserMapper() {}

  static AppUser toDomain(AppUserJpaEntity e) {
    return AppUser.restore(
        UserId.of(e.getId()),
        KeycloakUserSub.nullableOf(e.getKeycloakSub()),
        e.getUsername(),
        e.getEmail(),
        e.getPhone(),
        e.getFirstName(),
        e.getLastName(),
        e.getDisplayName(),
        e.getAvatarUrl(),
        e.getStatus(),
        e.getApprovedAt(),
        e.getApprovedBy(),
        e.getLastLoginAt(),
        e.getVersion());
  }

  static AppUserJpaEntity toNewEntity(AppUser u) {
    AppUserJpaEntity e = new AppUserJpaEntity();
    if (u.getId() != null) e.setId(u.getId().value());
    apply(e, u);
    return e;
  }

  static AppUserJpaEntity merge(AppUserJpaEntity e, AppUser u) {
    apply(e, u);
    return e;
  }

  private static void apply(AppUserJpaEntity e, AppUser u) {
    // map KeycloakUserSub wrapper to raw UUID for persistence
    e.setKeycloakSub(u.getKeycloakSub() == null ? null : u.getKeycloakSub().value());
    e.setUsername(u.getUsername());
    e.setEmail(u.getEmail());
    e.setPhone(u.getPhone());
    e.setFirstName(u.getFirstName());
    e.setLastName(u.getLastName());
    e.setDisplayName(u.getDisplayName());
    e.setAvatarUrl(u.getAvatarUrl());
    e.setStatus(u.getStatus());
    e.setApprovedAt(u.getApprovedAt());
    e.setApprovedBy(u.getApprovedBy());
    e.setLastLoginAt(u.getLastLoginAt());
    e.setVersion(u.getVersion());
  }
}
