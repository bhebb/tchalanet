package com.tchalanet.server.platform.identity.internal.persistence.mapper;

import com.tchalanet.server.platform.identity.api.model.TenantUserStatus;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.view.AppUserView;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.entity.TenantUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.entity.UserPreferenceJpaEntity;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import com.tchalanet.server.platform.identity.internal.model.TenantMembership;
import com.tchalanet.server.platform.identity.internal.model.UserPreference;

public final class IdentityPersistenceMapper {

  private IdentityPersistenceMapper() {}

  public static AppUserView toUserView(AppUserJpaEntity e, KeycloakUserSub keycloakSub) {
    return new AppUserView(
        UserId.of(e.getId()),
        e.getUsername(),
        e.getEmail(),
        e.getPhone(),
        e.getFirstName(),
        e.getLastName(),
        e.getDisplayName(),
        e.getStatus());
  }

  public static AppUser toUser(AppUserJpaEntity e, KeycloakUserSub keycloakSub) {
    return new AppUser(
        UserId.of(e.getId()),
        keycloakSub,
        e.getUsername(),
        e.getEmail(),
        e.getPhone(),
        e.getFirstName(),
        e.getLastName(),
        e.getDisplayName(),
        e.getAvatarUrl(),
        e.getStatus(),
        e.getApprovedAt(),
        UserId.nullableOf(e.getApprovedBy()),
        e.getLastLoginAt());
  }

  public static void merge(AppUserJpaEntity e, AppUser user) {
    if (user.id() != null) {
      e.setId(user.id().value());
    }
    e.setUsername(user.username());
    e.setEmail(user.email());
    e.setPhone(user.phone());
    e.setFirstName(user.firstName());
    e.setLastName(user.lastName());
    e.setDisplayName(user.displayName());
    e.setAvatarUrl(user.avatarUrl());
    e.setStatus(user.status());
    e.setApprovedAt(user.approvedAt());
    e.setApprovedBy(user.approvedBy() == null ? null : user.approvedBy().value());
    e.setLastLoginAt(user.lastLoginAt());
  }

  public static UserPreference toPreference(UserPreferenceJpaEntity e) {
    return new UserPreference(
        UserId.of(e.getUser().getId()),
        e.getThemeMode(),
        e.getDensity(),
        e.getLocale(),
        e.getTimeZone(),
        e.getCurrency());
  }

  public static TenantMembership toMembership(TenantUserJpaEntity e) {
    return new TenantMembership(
        TenantId.of(e.getTenantId()),
        UserId.of(e.getUserId()),
        OutletId.nullableOf(e.getOutletId()),
        TerminalId.nullableOf(e.getTerminalId()),
        e.getStatus() == null ? TenantUserStatus.ACTIVE : e.getStatus(),
        Boolean.TRUE.equals(e.getIsOwner()));
  }
}
