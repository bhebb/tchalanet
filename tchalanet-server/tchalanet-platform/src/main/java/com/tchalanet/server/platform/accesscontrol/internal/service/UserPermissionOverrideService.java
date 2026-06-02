package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.accesscontrol.api.model.request.DenyUserPermissionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GrantUserPermissionRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.RemoveUserPermissionOverrideRequest;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.UserPermissionOverrideJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserPermissionOverrideJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages per-user GRANT / DENY permission overrides ({@code user_permission_override}). */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPermissionOverrideService {

  private final UserPermissionOverrideJpaRepository overrideRepository;

  @Transactional
  public void grantUserPermission(GrantUserPermissionRequest request) {
    upsertOverride(request.tenantId(), request.userId().value(), request.permissionCode(),
        "GRANT", request.reason(), request.grantedBy() == null ? null : request.grantedBy().value());
  }

  @Transactional
  public void denyUserPermission(DenyUserPermissionRequest request) {
    upsertOverride(request.tenantId(), request.userId().value(), request.permissionCode(),
        "DENY", request.reason(), request.deniedBy() == null ? null : request.deniedBy().value());
  }

  @Transactional
  public void removeUserPermissionOverride(RemoveUserPermissionOverrideRequest request) {
    int removed = overrideRepository.softDelete(
        request.tenantId().value(), request.userId().value(), request.permissionCode());
    log.info("Removed {} permission override(s) for {} on {}", removed, request.userId(), request.permissionCode());
  }

  private void upsertOverride(TenantId tenantId, UUID userId, String code, String effect, String reason, UUID actorId) {
    // Soft-delete existing active override first (unique active constraint)
    overrideRepository.softDelete(tenantId.value(), userId, code);
    var entity = new UserPermissionOverrideJpaEntity();
    entity.setTenantId(tenantId.value());
    entity.setUserId(userId);
    entity.setPermissionCode(code);
    entity.setEffect(effect);
    entity.setReason(reason);
    entity.setCreatedBy(actorId);
    overrideRepository.save(entity);
    log.info("User {} permission override: {} {} by {}", userId, effect, code, actorId);
  }
}
