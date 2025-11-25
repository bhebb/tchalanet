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

public class TransactionLocalRlsTest {

  static PostgreSQLContainer<?> postgres;

  @BeforeAll
  public static void start() {
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
  }

  @AfterAll
  public static void stop() {
    if (postgres != null) postgres.stop();
  }

  @Test
  public void transactionLocalSetCurrentTenantDoesNotLeakAfterRollback() throws Exception {
    // open a connection and start a transaction
    try (Connection conn =
        DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      conn.setAutoCommit(false);
      try (Statement st = conn.createStatement()) {
        UUID tenant = UUID.randomUUID();
        // set transaction-local tenant
        st.execute("SELECT set_current_tenant('" + tenant.toString() + "')");
        try (ResultSet rs = st.executeQuery("SELECT current_setting('app.current_tenant', true)")) {
          Assertions.assertTrue(rs.next());
          String val = rs.getString(1);
          Assertions.assertEquals(tenant.toString(), val);
        }
        // rollback transaction
        conn.rollback();
      }
    }

    // new connection should not see the transaction-local setting
    try (Connection conn2 =
        DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      try (Statement st2 = conn2.createStatement()) {
        try (ResultSet rs2 =
            st2.executeQuery("SELECT current_setting('app.current_tenant', true)")) {
          // If no setting exists, current_setting(..., true) returns NULL, so rs2.next() true but
          // value is null
          Assertions.assertTrue(rs2.next());
          String val2 = rs2.getString(1);
          Assertions.assertNull(val2);
        }
      }
    }
  }
}
