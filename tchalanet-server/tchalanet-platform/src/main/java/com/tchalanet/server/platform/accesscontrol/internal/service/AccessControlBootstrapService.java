package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.accesscontrol.api.model.request.BootstrapAccessControlRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.BootstrapAccessControlRequest.BootstrapMode;
import com.tchalanet.server.platform.accesscontrol.api.model.result.BootstrapAccessControlResult;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRolePermissionId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.AppRolePermissionJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.PermissionJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PermissionJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.RolePermissionAdminJpaRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessControlBootstrapService {

  private static final String MATRIX_PATH = "access-control/default-role-permissions.v1.json";

  private final AppRoleJpaRepository roleRepository;
  private final PermissionJpaRepository permissionRepository;
  private final RolePermissionAdminJpaRepository rolePermissionRepository;
  private final JsonUtils jsonUtils;

  @Transactional
  public BootstrapAccessControlResult execute(BootstrapAccessControlRequest request) {
    var matrix = loadMatrix();
    return switch (request.mode()) {
      case VALIDATE -> validate(matrix);
      case APPLY_MISSING -> applyMissing(matrix);
      case SYNC_SYSTEM -> syncSystem(matrix);
    };
  }

  private BootstrapAccessControlResult validate(RolePermissionMatrix matrix) {
    var errors = new ArrayList<String>();
    var knownCodes = matrix.permissions().stream()
        .map(PermissionDef::code).collect(Collectors.toSet());
    for (var role : matrix.roles()) {
      if (role.custom()) errors.add("V1 does not support custom roles: " + role.code());
      for (var p : role.permissions()) {
        if (!knownCodes.contains(p)) errors.add("Role " + role.code() + " references unknown permission: " + p);
      }
    }
    return new BootstrapAccessControlResult(BootstrapMode.VALIDATE.name(), 0, 0, 0, 0, errors, errors.isEmpty());
  }

  private BootstrapAccessControlResult applyMissing(RolePermissionMatrix matrix) {
    int permsCreated = 0, rolesCreated = 0, mappingsCreated = 0;

    // 1. Create missing permissions
    var existingPerms = permissionRepository.findAll().stream()
        .map(PermissionJpaEntity::getCode).collect(Collectors.toSet());
    for (var pd : matrix.permissions()) {
      if (!existingPerms.contains(pd.code())) {
        var e = new PermissionJpaEntity();
        e.setCode(pd.code());
        e.setName(pd.label());
        e.setCategory(pd.category());
        e.setSystem(pd.system());
        e.setActive(pd.active());
        permissionRepository.save(e);
        permsCreated++;
      }
    }

    // 2. Create missing system roles
    for (var rd : matrix.roles()) {
      var existing = roleRepository.findByCode(rd.code());
      if (existing.isEmpty()) {
        var e = new AppRoleJpaEntity();
        e.setCode(rd.code());
        e.setName(rd.label());
        e.setScope(rd.scope());
        e.setSystem(rd.system());
        e.setCustom(rd.custom());
        e.setActive(true);
        roleRepository.save(e);
        rolesCreated++;
      }
    }

    // 3. Create missing role-permission links
    var allRoles = roleRepository.findAllGlobalNotDeleted().stream()
        .collect(Collectors.toMap(AppRoleJpaEntity::getCode, Function.identity()));
    var allPerms = permissionRepository.findAll().stream()
        .collect(Collectors.toMap(PermissionJpaEntity::getCode, Function.identity()));

    for (var rd : matrix.roles()) {
      var role = allRoles.get(rd.code());
      if (role == null) continue;
      var existingLinks = rolePermissionRepository.findByRoleId(role.getId()).stream()
          .map(l -> l.getPermission().getCode()).collect(Collectors.toSet());
      for (var pCode : rd.permissions()) {
        if (!existingLinks.contains(pCode) && allPerms.containsKey(pCode)) {
          var link = new AppRolePermissionJpaEntity();
          link.setId(new AppRolePermissionId(role.getId(), pCode));
          link.setRole(role);
          link.setPermission(allPerms.get(pCode));
          rolePermissionRepository.save(link);
          mappingsCreated++;
        }
      }
    }

    log.info("Bootstrap APPLY_MISSING: +{}p +{}r +{}m", permsCreated, rolesCreated, mappingsCreated);
    return new BootstrapAccessControlResult(BootstrapMode.APPLY_MISSING.name(), permsCreated, rolesCreated, mappingsCreated, 0, List.of(), true);
  }

  private BootstrapAccessControlResult syncSystem(RolePermissionMatrix matrix) {
    // Apply missing first, then remove extra links from system roles
    var result = applyMissing(matrix);
    int removed = 0;

    var matrixMappings = matrix.roles().stream()
        .collect(Collectors.toMap(RoleDef::code, rd -> (Set<String>) Set.copyOf(rd.permissions())));

    for (var role : roleRepository.findAllGlobalNotDeleted()) {
      if (!role.isSystem()) continue;
      var expected = matrixMappings.getOrDefault(role.getCode(), Set.of());
      var currentLinks = rolePermissionRepository.findByRoleId(role.getId());
      for (var link : currentLinks) {
        if (!expected.contains(link.getPermission().getCode())) {
          rolePermissionRepository.delete(link);
          removed++;
        }
      }
    }

    log.info("Bootstrap SYNC_SYSTEM: removed {} extra mappings", removed);
    return new BootstrapAccessControlResult(
        BootstrapMode.SYNC_SYSTEM.name(),
        result.permissionsCreated(), result.rolesCreated(), result.roleMappingsCreated(),
        removed, List.of(), true);
  }

  private RolePermissionMatrix loadMatrix() {
    try {
      ClassPathResource resource = new ClassPathResource(MATRIX_PATH);
      try (InputStream is = resource.getInputStream()) {
        return jsonUtils.readValue(is, RolePermissionMatrix.class);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load access-control matrix from " + MATRIX_PATH, e);
    }
  }

  // ─── JSON model ──────────────────────────────────────────────────────────
  // Unknown properties are ignored globally by the shared JsonMapper (JacksonConfig disables
  // FAIL_ON_UNKNOWN_PROPERTIES), so no per-type annotation is needed here.

  public record RolePermissionMatrix(List<PermissionDef> permissions, List<RoleDef> roles) {}

  public record PermissionDef(String code, String label, String category, boolean system, boolean active) {}

  public record RoleDef(String code, String label, String scope, boolean system, boolean custom, List<String> permissions) {}
}
