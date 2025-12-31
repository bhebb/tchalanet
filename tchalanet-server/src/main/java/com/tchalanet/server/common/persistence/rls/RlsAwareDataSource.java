package com.tchalanet.server.common.persistence.rls;

import com.tchalanet.server.common.context.TchContextResolver;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;

@Slf4j
public final class RlsAwareDataSource extends DelegatingDataSource {

  private final TchContextResolver resolver;

  public RlsAwareDataSource(DataSource targetDataSource, TchContextResolver resolver) {
    super(targetDataSource);
    this.resolver = resolver;
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection raw = super.getConnection();
    applyOrResetRls(raw);
    logRlsVars(raw, "after-apply");
    return ResetOnCloseConnection.wrap(raw, true);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    var raw = super.getConnection(username, password);
    applyOrResetRls(raw);
    logRlsVars(raw, "after-apply");
    return ResetOnCloseConnection.wrap(raw, true);
  }

  private void logRlsVars(Connection conn, String when) {
    try (var ps =
        conn.prepareStatement(
            """
                select
                  current_setting('app.current_tenant', true) as t,
                  current_setting('app.deleted_visibility', true) as v
            """)) {
      try (var rs = ps.executeQuery()) {
        rs.next();
        log.debug(
            "[RLS {}] current_tenant='{}' deleted_visibility='{}'",
            when,
            rs.getString("t"),
            rs.getString("v"));
      }
    } catch (Exception e) {
      log.debug("[RLS {}] cannot read current_setting", when, e);
    }
  }

  private void applyOrResetRls(Connection conn) {
    UUID tenantUuid = null;
    String visibility = "active";

    try {
      var ctx = resolver.currentOrNull();
      if (ctx == null) {
        tenantUuid = null;
        visibility = "active";
      } else {
        visibility = safeVisibility(ctx.deletedVisibilitySafe());
        tenantUuid = ctx.tenantUuid();
      }

    } catch (Exception e) {
      log.debug("RLS context read failed", e);
      tenantUuid = null;
      visibility = "active";
    }

    // reset if no tenant
    if (tenantUuid == null) {
      resetViaSetConfig(conn);
      // log current settings after reset
      logRlsVars(conn, "after-reset");
      return;
    }

    applyViaSetConfig(conn, tenantUuid, visibility);
    // log current settings after apply
    logRlsVars(conn, "after-apply-internal");
  }

  private void applyViaSetConfig(Connection conn, UUID tenantUuid, String visibility) {
    try (PreparedStatement stTenant =
            conn.prepareStatement("select set_config('app.current_tenant', ?, false)");
        PreparedStatement stVis =
            conn.prepareStatement("select set_config('app.deleted_visibility', ?, false)")) {

      stTenant.setString(1, tenantUuid.toString());
      stTenant.execute();

      stVis.setString(1, visibility);
      stVis.execute();

    } catch (Exception e) {
      log.error(
          "Failed to apply RLS via set_config (tenant={}, visibility={})",
          tenantUuid,
          visibility,
          e);
      resetViaSetConfig(conn);
      // log current settings after reset due to failure
      logRlsVars(conn, "after-reset-error");
    }
  }

  private void resetViaSetConfig(Connection conn) {
    try (PreparedStatement stTenant =
            conn.prepareStatement("select set_config('app.current_tenant', '', false)");
        PreparedStatement stVis =
            conn.prepareStatement("select set_config('app.deleted_visibility', 'active', false)")) {
      stTenant.execute();
      stVis.execute();
    } catch (Exception e) {
      log.debug("Failed to reset RLS via set_config", e);
    }
  }

  private String safeVisibility(String v) {
    if (v == null) return "active";
    String x = v.trim().toLowerCase();
    return (x.equals("active") || x.equals("deleted") || x.equals("all")) ? x : "active";
  }
}
