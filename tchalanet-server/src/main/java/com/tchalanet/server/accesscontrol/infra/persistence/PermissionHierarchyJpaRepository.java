package com.tchalanet.server.accesscontrol.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PermissionHierarchyJpaRepository
    extends JpaRepository<AppRolePermissionEntity, AppRolePermissionId> {

  @Query(
      value =
          """
                    WITH RECURSIVE role_tree AS (
                        SELECT ar.id, ar.parent_role_id
                        FROM app_role ar
                        WHERE ar.id = :roleId
                          AND ar.deleted_at IS NULL

                        UNION ALL

                        SELECT parent.id, parent.parent_role_id
                        FROM app_role parent
                        JOIN role_tree rt ON parent.id = rt.parent_role_id
                        WHERE parent.deleted_at IS NULL
                    )
                    SELECT DISTINCT rp.permission_code
                    FROM role_tree rt
                    JOIN role_permission rp ON rp.role_id = rt.id
                    """,
      nativeQuery = true)
  List<String> findPermissionCodesForRoleHierarchy(UUID roleId);
}
