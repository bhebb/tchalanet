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

public class RlsIntegrationTest {

  static PostgreSQLContainer<?> postgres;
  private static final String APP_USER = "app";
  private static final String APP_PWD = "app";

  @BeforeAll
  public static void start() throws Exception {
    postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.4-alpine"))
            .withDatabaseName("tchalanet_test")
            .withUsername("test")
            .withPassword("test");
    postgres.start();

    // Run Flyway migrations against the container (apply all available migrations)
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .load();
    flyway.migrate();

    // Create a non-superuser role for tests so RLS is applied (superusers bypass RLS)
    try (Connection admin =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement st = admin.createStatement()) {
      st.execute("CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PWD + "' NOSUPERUSER");
      st.execute("GRANT CONNECT ON DATABASE " + postgres.getDatabaseName() + " TO " + APP_USER);
      st.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
      st.execute(
          "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + APP_USER);
      st.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + APP_USER);
      // Ensure future created objects also grant rights
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

  @Test
  public void testSetCurrentTenantSessionVariable() throws Exception {
    try (Connection conn = appConn()) {
      // Use an explicit transaction so transaction-local settings (is_local = true) remain visible
      conn.setAutoCommit(false);
      try (Statement st = conn.createStatement()) {
        UUID tenant = UUID.randomUUID();
        st.execute("SELECT set_current_tenant('" + tenant.toString() + "')");
        try (ResultSet rs = st.executeQuery("SELECT current_setting('app.current_tenant', true)")) {
          Assertions.assertTrue(rs.next());
          String val = rs.getString(1);
          Assertions.assertEquals(tenant.toString(), val);
        }
        conn.commit();
      }
    }
  }
}
