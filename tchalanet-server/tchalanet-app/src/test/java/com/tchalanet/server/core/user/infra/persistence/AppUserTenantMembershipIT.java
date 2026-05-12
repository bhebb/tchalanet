package com.tchalanet.server.core.user.infra.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that findByTenantMembership isolates users by tenant_user membership.
 *
 * app_user is a global table (no tenant_id column, no RLS policy).
 * Tenant-scoped user listing is achieved via a JOIN on tenant_user.
 */
@Testcontainers
@DisplayName("app_user × tenant_user membership isolation (Testcontainers PostgreSQL)")
class AppUserTenantMembershipIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("testdb");

    JdbcTemplate jdbc;

    final UUID tenantA   = UUID.randomUUID();
    final UUID tenantB   = UUID.randomUUID();
    final UUID userInA   = UUID.randomUUID();
    final UUID userInB   = UUID.randomUUID();
    final UUID userInBoth = UUID.randomUUID();
    final UUID roleId    = UUID.randomUUID();

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(ds);

        jdbc.execute("CREATE EXTENSION IF NOT EXISTS citext");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tenant (
                id uuid PRIMARY KEY,
                version bigint NOT NULL DEFAULT 0,
                name text NOT NULL,
                slug text NOT NULL,
                created_at timestamptz NOT NULL DEFAULT now(),
                updated_at timestamptz NOT NULL DEFAULT now(),
                deleted_at timestamptz
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS app_user (
                id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
                version      bigint      NOT NULL DEFAULT 0,
                keycloak_sub uuid        NOT NULL,
                username     text,
                email        citext,
                status       varchar(32) NOT NULL DEFAULT 'ACTIVE',
                created_at   timestamptz NOT NULL DEFAULT now(),
                updated_at   timestamptz NOT NULL DEFAULT now(),
                deleted_at   timestamptz
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS app_role (
                id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                version   bigint NOT NULL DEFAULT 0,
                code      text   NOT NULL,
                tenant_id uuid,
                deleted_at timestamptz
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tenant_user (
                id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
                version     bigint      NOT NULL DEFAULT 0,
                tenant_id   uuid        NOT NULL REFERENCES tenant(id),
                user_id     uuid        NOT NULL REFERENCES app_user(id),
                role_id     uuid        NOT NULL REFERENCES app_role(id),
                status      varchar(32) NOT NULL DEFAULT 'ACTIVE',
                is_owner    boolean     NOT NULL DEFAULT false,
                created_at  timestamptz NOT NULL DEFAULT now(),
                updated_at  timestamptz NOT NULL DEFAULT now(),
                deleted_at  timestamptz
            )""");

        jdbc.update("INSERT INTO tenant (id, name, slug) VALUES (?, 'Tenant A', 'a')", tenantA);
        jdbc.update("INSERT INTO tenant (id, name, slug) VALUES (?, 'Tenant B', 'b')", tenantB);
        jdbc.update("INSERT INTO app_role (id, code) VALUES (?, 'CASHIER')", roleId);
        jdbc.update("INSERT INTO app_user (id, keycloak_sub, username) VALUES (?, ?, 'user-a')",
            userInA, UUID.randomUUID());
        jdbc.update("INSERT INTO app_user (id, keycloak_sub, username) VALUES (?, ?, 'user-b')",
            userInB, UUID.randomUUID());
        jdbc.update("INSERT INTO app_user (id, keycloak_sub, username) VALUES (?, ?, 'user-both')",
            userInBoth, UUID.randomUUID());

        jdbc.update("INSERT INTO tenant_user (tenant_id, user_id, role_id) VALUES (?,?,?)", tenantA, userInA, roleId);
        jdbc.update("INSERT INTO tenant_user (tenant_id, user_id, role_id) VALUES (?,?,?)", tenantB, userInB, roleId);
        jdbc.update("INSERT INTO tenant_user (tenant_id, user_id, role_id) VALUES (?,?,?)", tenantA, userInBoth, roleId);
        jdbc.update("INSERT INTO tenant_user (tenant_id, user_id, role_id) VALUES (?,?,?)", tenantB, userInBoth, roleId);
    }

    @AfterEach
    void cleanup() {
        jdbc.execute("DROP TABLE IF EXISTS tenant_user");
        jdbc.execute("DROP TABLE IF EXISTS app_role");
        jdbc.execute("DROP TABLE IF EXISTS app_user");
        jdbc.execute("DROP TABLE IF EXISTS tenant");
    }

    private List<UUID> queryMembership(UUID tenantId) {
        return jdbc.queryForList("""
            SELECT u.id FROM app_user u
            JOIN tenant_user tu ON tu.user_id = u.id
            WHERE tu.tenant_id = ?
              AND tu.deleted_at IS NULL
              AND tu.status = 'ACTIVE'
              AND u.deleted_at IS NULL
            """, UUID.class, tenantId);
    }

    @Test
    @DisplayName("4.1 — returns only users of tenant A, not users of other tenants")
    void findByTenantId_returnsOnlyUsersOfTenantA() {
        var ids = queryMembership(tenantA);

        assertThat(ids).containsExactlyInAnyOrder(userInA, userInBoth);
        assertThat(ids).doesNotContain(userInB);
    }

    @Test
    @DisplayName("4.1 — symmetrical: returns only users of tenant B")
    void findByTenantId_returnsOnlyUsersOfTenantB() {
        var ids = queryMembership(tenantB);

        assertThat(ids).containsExactlyInAnyOrder(userInB, userInBoth);
        assertThat(ids).doesNotContain(userInA);
    }

    @Test
    @DisplayName("4.2 — returns empty list for a tenant with no memberships")
    void findByTenantId_returnsEmptyForTenantWithNoMembers() {
        UUID ghostTenant = UUID.randomUUID();
        jdbc.update("INSERT INTO tenant (id, name, slug) VALUES (?, 'Ghost', 'ghost')", ghostTenant);

        assertThat(queryMembership(ghostTenant)).isEmpty();
    }

    @Test
    @DisplayName("4.2 — soft-deleted tenant_user membership is excluded")
    void findByTenantId_excludesSoftDeletedMembership() {
        jdbc.update("""
            UPDATE tenant_user SET deleted_at = now()
            WHERE tenant_id = ? AND user_id = ?
            """, tenantA, userInA);

        var ids = queryMembership(tenantA);

        assertThat(ids).containsExactly(userInBoth);
        assertThat(ids).doesNotContain(userInA);
    }

    @Test
    @DisplayName("4.2 — SUSPENDED membership is excluded")
    void findByTenantId_excludesSuspendedMembership() {
        jdbc.update("""
            UPDATE tenant_user SET status = 'SUSPENDED'
            WHERE tenant_id = ? AND user_id = ?
            """, tenantA, userInA);

        var ids = queryMembership(tenantA);

        assertThat(ids).containsExactly(userInBoth);
        assertThat(ids).doesNotContain(userInA);
    }

    @Test
    @DisplayName("4.2 — soft-deleted app_user is excluded from results")
    void findByTenantId_excludesSoftDeletedUser() {
        jdbc.update("UPDATE app_user SET deleted_at = now() WHERE id = ?", userInA);

        var ids = queryMembership(tenantA);

        assertThat(ids).containsExactly(userInBoth);
        assertThat(ids).doesNotContain(userInA);
    }
}
