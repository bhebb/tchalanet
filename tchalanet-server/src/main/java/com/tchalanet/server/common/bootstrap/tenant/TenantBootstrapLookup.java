package com.tchalanet.server.common.bootstrap.tenant;

import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** BOOTSTRAP ONLY. Bypasses RLS. Do not use in business services. */
@Component
public class TenantBootstrapLookup {

  private final JdbcTemplate jdbc;

  public TenantBootstrapLookup(@Qualifier("rawDataSource") DataSource rawDataSource) {
    this.jdbc = new JdbcTemplate(rawDataSource);
  }

  public Optional<UUID> findTenantUuidByCode(String code) {
    if (code == null || code.isBlank()) return Optional.empty();
    try {
      final String sql = "select id from tenant where code = ? and deleted_at is null limit 1";
      UUID id =
          jdbc.query(
              sql, ps -> ps.setString(1, code), rs -> rs.next() ? (UUID) rs.getObject(1) : null);
      return Optional.ofNullable(id);
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
