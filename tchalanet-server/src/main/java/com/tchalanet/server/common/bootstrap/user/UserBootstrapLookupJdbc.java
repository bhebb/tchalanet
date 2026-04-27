package com.tchalanet.server.common.bootstrap.user;

import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** BOOTSTRAP ONLY. Bypasses RLS. Do not use in business services. */
@Component
public class UserBootstrapLookupJdbc implements UserBootstrapLookup {

  private final JdbcTemplate jdbc;

  public UserBootstrapLookupJdbc(@Qualifier("rawDataSource") DataSource rawDataSource) {
    this.jdbc = new JdbcTemplate(rawDataSource);
  }

  @Override
  public Optional<UUID> findAppUserIdByKeycloakSub(UUID keycloakSub) {
    if (keycloakSub == null) return Optional.empty();
    try {
      String sql = "select id from app_user where keycloak_sub = ? and deleted_at is null limit 1";
      UUID id =
          jdbc.query(
              sql,
              ps -> ps.setObject(1, keycloakSub),
              rs -> rs.next() ? (UUID) rs.getObject(1) : null);
      return Optional.ofNullable(id);
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
