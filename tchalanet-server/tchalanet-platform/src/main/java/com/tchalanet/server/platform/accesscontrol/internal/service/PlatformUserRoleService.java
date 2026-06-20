package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.PlatformUserRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformSuperAdminRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformUserRoleJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformUserRoleService {

  private static final String PLATFORM_ROLE_SCOPE = "PLATFORM";
  private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

  private final AppRoleJpaRepository appRoleRepository;
  private final PlatformUserRoleJpaRepository platformUserRoleRepository;

  @Transactional(readOnly = true)
  public List<PlatformSuperAdminRow> listSuperAdmins() {
    return platformUserRoleRepository.listActiveSuperAdmins();
  }

  @Transactional
  public void assignSuperAdmin(UserId userId, UserId assignedBy) {
    var role = appRoleRepository.findActiveSystemRoleByCodeAndScope(SUPER_ADMIN_ROLE, PLATFORM_ROLE_SCOPE)
        .orElseThrow(() -> new IllegalStateException("Platform role not found: " + SUPER_ADMIN_ROLE));
    if (platformUserRoleRepository.findActiveAssignment(userId.value(), role.getId()).isPresent()) {
      return;
    }
    var entity = new PlatformUserRoleJpaEntity();
    entity.setUserId(userId.value());
    entity.setRoleId(role.getId());
    entity.setAssignedBy(assignedBy == null ? null : assignedBy.value());
    platformUserRoleRepository.save(entity);
  }

  @Transactional
  public void removeSuperAdmin(UserId userId) {
    var role = appRoleRepository.findActiveSystemRoleByCodeAndScope(SUPER_ADMIN_ROLE, PLATFORM_ROLE_SCOPE)
        .orElseThrow(() -> new IllegalStateException("Platform role not found: " + SUPER_ADMIN_ROLE));
    platformUserRoleRepository.softDeleteAssignment(userId.value(), role.getId());
  }
}
