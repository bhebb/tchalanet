package com.tchalanet.server.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class RlsPoliciesIntegrationTest {

  static PostgreSQLContainer<?> postgres;
  private static final String APP_USER = "app";
  private static final String APP_PWD = "app";

  // seeded tenants in V4: platform = 00000000-0000-0000-0000-000000000001, demo =
  // 00000000-0000-0000-0000-000000000002
  private static final String TENANT_PLATFORM = "00000000-0000-0000-0000-000000000001";
  private static final String TENANT_DEMO = "00000000-0000-0000-0000-000000000002";

  @BeforeAll
  public static void start() throws Exception {
    postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.4-alpine"))
            .withDatabaseName("tchalanet_test")
            .withUsername("test")
            .withPassword("test");
    postgres.start();

    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .load();
    flyway.migrate();

    // create app user if missing (idempotent)
    try (Connection admin =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement st = admin.createStatement()) {
      st.execute(
          "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '"
              + APP_USER
              + "') THEN CREATE ROLE "
              + APP_USER
              + " LOGIN PASSWORD '"
              + APP_PWD
              + "' NOSUPERUSER; END IF; END$$;");
      st.execute("GRANT CONNECT ON DATABASE " + postgres.getDatabaseName() + " TO " + APP_USER);
      st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
      st.execute(
          "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + APP_USER);
      st.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + APP_USER);
      st.execute(
          "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "
              + APP_USER);
    }
  }

  @AfterAll
  public static void stop() {
    if (postgres != null) postgres.stop();
  }

  private Connection appConn() throws Exception {
    return DriverManager.getConnection(postgres.getJdbcUrl(), APP_USER, APP_PWD);
  }

  private Connection adminConn() throws Exception {
    return DriverManager.getConnection(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
  }

  @Test
  public void tenantIsolationAndDeletedVisibility() throws Exception {
    UUID outletId = UUID.randomUUID();
    UUID terminalId = UUID.randomUUID();
    String ticketCode1 = "RLSTEST-" + UUID.randomUUID();
    String ticketCode2 = "RLSTEST-" + UUID.randomUUID();

    // 1) Create outlet, terminal and ticket for TENANT_DEMO using admin connection (bypass RLS for
    // creation)
    try (Connection conn = adminConn()) {
      try (Statement st = conn.createStatement()) {
        st.execute(
            "INSERT INTO outlet (id, tenant_id, name, zone, created_at, updated_at) VALUES ('"
                + outletId
                + "', '"
                + TENANT_DEMO
                + "', 'rls-outlet', 'z', now(), now())");

        st.execute(
            "INSERT INTO terminal (id, tenant_id, outlet_id, state, created_at, updated_at) VALUES ('"
                + terminalId
                + "', '"
                + TENANT_DEMO
                + "', '"
                + outletId
                + "', 'ACTIVE', now(), now())");

        st.execute(
            "INSERT INTO ticket (id, tenant_id, terminal_id, ticket_code, status, total_amount, created_at, updated_at) VALUES ('"
                + UUID.randomUUID()
                + "', '"
                + TENANT_DEMO
                + "', '"
                + terminalId
                + "', '"
                + ticketCode1
                + "', 'PENDING', 10.00, now(), now())");
      }
    }

    // admin verify the ticket exists
    try (Connection admin = adminConn();
        Statement st = admin.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT tenant_id::text, ticket_code FROM ticket WHERE ticket_code = '"
                    + ticketCode1
                    + "'")) {
      if (rs.next()) {
        System.out.println(
            "ADMIN TICKET ROW: tenant_id=" + rs.getString(1) + " code=" + rs.getString(2));
      } else {
        System.out.println("ADMIN: ticket not found after insert!\n");
      }
    }

    // 2) Verify tenant B (platform) cannot see the ticket using app user
    try (Connection conn = appConn()) {
      conn.setAutoCommit(false);
      try (Statement st = conn.createStatement()) {
        st.execute("SELECT set_current_tenant('" + TENANT_PLATFORM + "')");
        try (ResultSet rs = st.executeQuery("SELECT current_setting('app.current_tenant', true)")) {
          System.out.println(
              "APP current_tenant (platform): " + (rs.next() ? rs.getString(1) : "<no-setting>"));
        }
        try (ResultSet rs =
            st.executeQuery(
                "SELECT count(*) FROM ticket WHERE ticket_code = '" + ticketCode1 + "'")) {
          Assertions.assertTrue(rs.next());
          Assertions.assertEquals(0, rs.getInt(1));
        }
        conn.commit();
      }
    }

    // 3) Verify tenant A (demo) can see the ticket
    try (Connection conn = appConn()) {
      conn.setAutoCommit(false);
      try (Statement st = conn.createStatement()) {
        st.execute("SELECT set_current_tenant('" + TENANT_DEMO + "')");
        try (ResultSet rs = st.executeQuery("SELECT current_setting('app.current_tenant', true)")) {
          System.out.println(
              "APP current_tenant (demo): " + (rs.next() ? rs.getString(1) : "<no-setting>"));
        }
        try (ResultSet rs =
            st.executeQuery(
                "SELECT tenant_id::text, ticket_code FROM ticket WHERE ticket_code = '"
                    + ticketCode1
                    + "'")) {
          if (rs.next()) {
            System.out.println(
                "APP SEE ROWS: tenant_id=" + rs.getString(1) + " code=" + rs.getString(2));
          } else {
            System.out.println("APP: no rows visible for demo tenant");
          }
        }

        // diagnostic: compare tenant_id vs current_setting for the ticket row
        try (ResultSet dr =
            st.executeQuery(
                "SELECT tenant_id::text as tid, current_setting('app.current_tenant', true) as cur, (tenant_id = current_setting('app.current_tenant', true)::uuid) as match FROM ticket WHERE ticket_code = '"
                    + ticketCode1
                    + "'")) {
          if (dr.next()) {
            System.out.println(
                "DIAG: tenant_id="
                    + dr.getString("tid")
                    + " current_setting="
                    + dr.getString("cur")
                    + " match="
                    + dr.getString("match"));
          } else {
            System.out.println("DIAG: no ticket row to compare for " + ticketCode1);
          }
        }
        try (ResultSet rs =
            st.executeQuery(
                "SELECT count(*) FROM ticket WHERE ticket_code = '" + ticketCode1 + "'")) {
          Assertions.assertTrue(rs.next());
          Assertions.assertEquals(1, rs.getInt(1));
        }
        conn.commit();
      }
    }

    // 4) Create a deleted ticket for tenant demo using admin
    try (Connection conn = adminConn()) {
      try (Statement st = conn.createStatement()) {
        st.execute(
            "INSERT INTO ticket (id, tenant_id, terminal_id, ticket_code, status, total_amount, created_at, updated_at, deleted_at) VALUES ('"
                + UUID.randomUUID()
                + "', '"
                + TENANT_DEMO
                + "', '"
                + terminalId
                + "', '"
                + ticketCode2
                + "', 'PENDING', 5.00, now(), now(), now())");
      }
    }

    // admin verify both tickets
    try (Connection admin = adminConn();
        Statement st = admin.createStatement();
        ResultSet rs =
            st.executeQuery(
                "SELECT tenant_id::text, ticket_code, deleted_at IS NOT NULL FROM ticket WHERE tenant_id = '"
                    + TENANT_DEMO
                    + "'")) {
      System.out.println("ADMIN TICKETS FOR DEMO:");
      while (rs.next()) {
        System.out.println(
            " - tenant_id="
                + rs.getString(1)
                + " code="
                + rs.getString(2)
                + " deleted="
                + rs.getBoolean(3));
      }
    }

    // 5) With deleted_visibility = 'active' (default), only non-deleted tickets visible
    try (Connection conn = appConn()) {
      conn.setAutoCommit(false);
      try (Statement st = conn.createStatement()) {
        st.execute("SELECT set_current_tenant('" + TENANT_DEMO + "')");
        st.execute("SELECT set_deleted_visibility('active')");
        try (ResultSet rs =
            st.executeQuery(
                "SELECT count(*) FROM ticket WHERE tenant_id = '" + TENANT_DEMO + "'")) {
          Assertions.assertTrue(rs.next());
          // should count only ticketCode1 (non-deleted)
          Assertions.assertEquals(1, rs.getInt(1));
        }
        conn.commit();
      }
    }

    // 6) With deleted_visibility = 'all' should see both
    try (Connection conn = appConn()) {
      conn.setAutoCommit(false);
      try (Statement st = conn.createStatement()) {
        st.execute("SELECT set_current_tenant('" + TENANT_DEMO + "')");
        st.execute("SELECT set_deleted_visibility('all')");
        try (ResultSet rs =
            st.executeQuery(
                "SELECT count(*) FROM ticket WHERE tenant_id = '" + TENANT_DEMO + "'")) {
          Assertions.assertTrue(rs.next());
          Assertions.assertEquals(2, rs.getInt(1));
        }
        conn.commit();
      }
    }
  }
}
