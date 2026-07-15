package com.tchalanet.server.core.sellerterminal.internal.infra.identity;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.SellerTerminalIdentityLookup;
import com.tchalanet.server.platform.identity.api.model.SellerTerminalBootstrapStatus;
import com.tchalanet.server.platform.identity.api.model.SellerTerminalIdentityBootstrapView;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CoreSellerTerminalIdentityLookupAdapter implements SellerTerminalIdentityLookup {

  private static final String SELECT =
      """
      SELECT t.id, t.tenant_id, t.status
      FROM seller_terminal t
      JOIN seller_terminal_external_identity e ON e.seller_terminal_id = t.id
      WHERE e.provider = ?
        AND e.issuer = ?
        AND e.external_subject = ?
        AND t.deleted_at IS NULL
        AND e.deleted_at IS NULL
      LIMIT 1
      """;

  private final JdbcTemplate jdbc;

  CoreSellerTerminalIdentityLookupAdapter(@Qualifier("rawDataSource") DataSource rawDataSource) {
    this.jdbc = new JdbcTemplate(rawDataSource);
  }

  @Override
  public Optional<SellerTerminalIdentityBootstrapView> findByExternalIdentity(
      IdentityProviderType provider, String issuer, String externalSubject) {
    return jdbc.execute(
        (ConnectionCallback<Optional<SellerTerminalIdentityBootstrapView>>)
            conn -> {
              bindPlatformReadScope(conn);
              try (var ps = conn.prepareStatement(SELECT)) {
                ps.setString(1, provider.name());
                ps.setString(2, issuer);
                ps.setString(3, externalSubject);
                try (var rs = ps.executeQuery()) {
                  if (!rs.next()) {
                    return Optional.empty();
                  }
                  return Optional.of(
                      new SellerTerminalIdentityBootstrapView(
                          SellerTerminalId.of((UUID) rs.getObject("id")),
                          TenantId.of((UUID) rs.getObject("tenant_id")),
                          toBootstrapStatus(SellerTerminalStatus.valueOf(rs.getString("status")))));
                }
              } finally {
                resetRlsScope(conn);
              }
            });
  }

  private void bindPlatformReadScope(Connection conn) throws SQLException {
    try (var tenant = conn.prepareStatement("select set_config('app.current_tenant', '', false)");
        var visibility =
            conn.prepareStatement("select set_config('app.deleted_visibility', 'active', false)");
        var scope = conn.prepareStatement("select set_config('app.api_scope', 'platform', false)");
        var superAdmin =
            conn.prepareStatement("select set_config('app.is_super_admin', 'true', false)")) {
      tenant.execute();
      visibility.execute();
      scope.execute();
      superAdmin.execute();
    }
  }

  private void resetRlsScope(Connection conn) throws SQLException {
    try (var reset = conn.prepareStatement("select public.reset_rls_context()")) {
      reset.execute();
    }
  }

  private SellerTerminalBootstrapStatus toBootstrapStatus(SellerTerminalStatus status) {
    return switch (status) {
      case ACTIVE -> SellerTerminalBootstrapStatus.ACTIVE;
      case BLOCKED -> SellerTerminalBootstrapStatus.BLOCKED;
      case DISABLED -> SellerTerminalBootstrapStatus.DISABLED;
      case PENDING -> SellerTerminalBootstrapStatus.SUSPENDED;
    };
  }
}
