package com.tchalanet.server.platform.identity.internal.adapter;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformUserRoleJpaRepository;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.result.BootstrapUserResult;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import com.tchalanet.server.platform.identity.api.model.view.AppUserView;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserJpaRepository;
import com.tchalanet.server.platform.identity.internal.persistence.repository.TenantUserJpaRepository;
import com.tchalanet.server.platform.identity.internal.service.CurrentUserProfileService;
import com.tchalanet.server.platform.identity.internal.service.TenantUserProvisioningService;
import com.tchalanet.server.platform.identity.internal.service.UserBootstrapService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter implementing the public {@link IdentityApi} contract.
 *
 * <p>Bridges the public module API to the internal identity services. Holds no business logic of
 * its own — it only delegates to the capability's internal services.
 */
@Component
@RequiredArgsConstructor
public class IdentityApiAdapter implements IdentityApi {

  private final CurrentUserProfileService profiles;
  private final UserBootstrapService bootstrapService;
  private final AppUserJpaAdapter appUsers;
  private final AppUserJpaRepository appUserRepository;
  private final TenantUserJpaRepository tenantUserRepository;
  private final PlatformUserRoleJpaRepository platformUserRoleRepository;
  private final TenantUserProvisioningService provisioningService;

  @Override
  public CurrentUserView getCurrentUser(GetCurrentUserRequest request) {
    return profiles.getCurrentUser(request.userId());
  }

  @Override
  public BootstrapUserResult bootstrapCurrentUser(BootstrapCurrentUserRequest request) {
    return bootstrapService.bootstrap(request);
  }

  @Override
  public UserProfileView getUserProfile(GetUserProfileRequest request) {
    return profiles.getUserProfile(request.userId());
  }

  @Override
  public Optional<AppUserView> findAppUser(UUID userId) {
    return appUsers
        .findById(com.tchalanet.server.common.types.id.UserId.of(userId))
        .map(
            user ->
                new AppUserView(
                    user.id(),
                    user.username(),
                    user.email(),
                    user.phone(),
                    user.firstName(),
                    user.lastName(),
                    user.displayName(),
                    user.status()));
  }

  @Override
  public List<AppUserView> listPlatformAdminsForNotificationDelivery() {
    var ids =
        platformUserRoleRepository.listActiveSuperAdmins().stream()
            .filter(row -> "ACTIVE".equals(row.getStatus()))
            .map(row -> row.getUserId())
            .toList();
    if (ids.isEmpty()) {
      return List.of();
    }
    return appUserRepository.findAllById(ids).stream()
        .filter(user -> user.getDeletedAt() == null)
        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
        .map(this::toView)
        .toList();
  }

  @Override
  public List<AppUserView> listTenantAdminsForNotificationDelivery(TenantId tenantId) {
    if (tenantId == null) {
      return List.of();
    }
    return appUserRepository.findTenantAdminsForNotificationDelivery(tenantId.value()).stream()
        .map(this::toView)
        .toList();
  }

  @Override
  public List<AppUserView> listTenantUsersForNotificationDelivery(TenantId tenantId) {
    if (tenantId == null) {
      return List.of();
    }
    return appUserRepository.findTenantUsersForNotificationDelivery(tenantId.value()).stream()
        .map(this::toView)
        .toList();
  }

  @Override
  public long countTenantUsers() {
    return tenantUserRepository.count();
  }

  @Override
  @Transactional
  public CreateUserResult createTenantUser(
      TenantId tenantId,
      String tenantCode,
      String email,
      String firstName,
      String lastName,
      TchRole role) {
    return provisioningService.provisionTenantUser(tenantId, tenantCode, email, firstName, lastName, role);
  }

  private AppUserView toView(AppUserJpaEntity user) {
    return new AppUserView(
        UserId.of(user.getId()),
        user.getUsername(),
        user.getEmail(),
        user.getPhone(),
        user.getFirstName(),
        user.getLastName(),
        user.getDisplayName(),
        user.getStatus());
  }
}
