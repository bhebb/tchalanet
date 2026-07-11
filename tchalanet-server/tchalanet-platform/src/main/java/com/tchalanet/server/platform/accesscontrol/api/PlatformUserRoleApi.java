package com.tchalanet.server.platform.accesscontrol.api;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.api.model.PlatformSuperAdminAccessRow;
import com.tchalanet.server.platform.accesscontrol.api.model.TenantAdminGlobalAccessRow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformUserRoleApi {

  List<PlatformSuperAdminAccessRow> listSuperAdmins();

  List<UUID> listActiveSuperAdminUserIds();

  List<TenantAdminGlobalAccessRow> searchTenantAdmins(String q, int page, int size, String sort);

  Optional<TenantAdminGlobalAccessRow> findTenantAdmin(UserId userId);

  long countTenantAdmins(String q);

  boolean hasPlatformRole(UserId userId);

  void assignSuperAdmin(UserId userId, UserId assignedBy);

  void removeSuperAdmin(UserId userId);
}
