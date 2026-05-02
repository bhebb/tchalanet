package com.tchalanet.server.core.tenantuser.infra.persistence;

import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.core.accesscontrol.domain.model.TenantUserSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcTenantUserDirectoryAdapter implements TenantUserDirectoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<TenantUserSnapshot> findActiveMembership(TenantId tenantId, UserId userId) {
        var rows =
            jdbc.query(
                """
                    select
                      tu.id,
                      tu.tenant_id,
                      tu.user_id,
                      tu.role_id,
                      tu.status
                    from tenant_user tu
                    where tu.tenant_id = ?
                      and tu.user_id = ?
                      and tu.status = 'ACTIVE'
                      and tu.deleted_at is null
                    limit 1
                    """,
                (rs, rowNum) -> toSnapshot(rs),
                tenantId.value(),
                userId.value());

        return rows.stream().findFirst();
    }

    @Override
    public List<RoleId> getUserRolesInTenant(UserId userId, TenantId tenantId) {
        return jdbc.query(
            """
                select tu.role_id
                from tenant_user tu
                where tu.tenant_id = ?
                  and tu.user_id = ?
                  and tu.status = 'ACTIVE'
                  and tu.deleted_at is null
                order by tu.created_at asc
                """,
            (rs, rowNum) -> RoleId.of(rs.getObject("role_id", UUID.class)),
            tenantId.value(),
            userId.value());
    }

    //to adapt
    private static TenantUserSnapshot toSnapshot(ResultSet rs) throws SQLException {
        return new TenantUserSnapshot(
            TenantId.of(rs.getObject("tenant_id", UUID.class)),
            UserId.of(rs.getObject("user_id", UUID.class)),

            RoleId.of(rs.getObject("role_id", UUID.class)),
            AutonomyLevel.PARTIAL, false);
    }
}
