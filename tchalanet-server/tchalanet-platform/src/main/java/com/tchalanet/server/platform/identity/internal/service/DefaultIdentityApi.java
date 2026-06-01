package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.identity.api.IdentityApi;
import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.request.GetUserProfileRequest;
import com.tchalanet.server.platform.identity.api.model.result.BootstrapUserResult;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import com.tchalanet.server.platform.identity.api.model.view.AppUserView;
import com.tchalanet.server.platform.identity.api.model.view.CurrentUserView;
import com.tchalanet.server.platform.identity.api.model.view.UserProfileView;
import com.tchalanet.server.platform.identity.internal.persistence.mapper.IdentityPersistenceMapper;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserJpaRepository;
import com.tchalanet.server.platform.identity.internal.persistence.repository.TenantUserJpaRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultIdentityApi implements IdentityApi {

  private final CurrentUserProfileService profiles;
  private final UserBootstrapService bootstrapService;
  private final AppUserJpaRepository appUserRepository;
  private final TenantUserJpaRepository tenantUserRepository;
  private final UserAdminService userAdminService;
  private final TenantMembershipService tenantMembershipService;

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
    return appUserRepository.findById(userId).map(IdentityPersistenceMapper::toUserView);
  }

  @Override
  public long countTenantUsers() {
    return tenantUserRepository.count();
  }

  @Override
  @Transactional
  public CreateUserResult createTenantUser(
      TenantId tenantId,
      String email,
      String firstName,
      String lastName,
      TchRole role) {
    var created = userAdminService.createUser(
        email, null, firstName, lastName,
        null, null, null, null, null,
        false, Set.of());
    tenantMembershipService.assign(tenantId, created.userId(), null, null, null, false);
    tenantMembershipService.setRole(tenantId, created.userId(), role);
    return created;
  }
}
