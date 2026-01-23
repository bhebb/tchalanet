package com.tchalanet.server.common.bootstrap.tenant;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * BOOTSTRAP ONLY. Bypasses RLS.
 *
 * This component reads tenant information directly from the database
 * without RLS filtering, for use during application bootstrap and
 * authentication flows where the tenant context is not yet established.
 *
 * DO NOT USE in business services - use proper ports/repositories instead.
 *
 * Uses rawDataSource to bypass RLS (no tenant_id in session).
 */
@Component
public class TenantBootstrapLookup {

  private final JdbcTemplate jdbc;

  public TenantBootstrapLookup(@Qualifier("rawDataSource") DataSource rawDataSource) {
    this.jdbc = new JdbcTemplate(rawDataSource);
  }

  /**
   * Find tenant UUID by tenant code.
   * Bypasses RLS - use only during bootstrap.
   */
  public Optional<UUID> findTenantUuidByCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }

    try {
      final String sql = "SELECT id FROM tenant WHERE code = ? AND deleted_at IS NULL LIMIT 1";
      UUID id =
          jdbc.query(
              sql,
              ps -> ps.setString(1, code),
              rs -> rs.next() ? (UUID) rs.getObject(1) : null);
      return Optional.ofNullable(id);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Find tenant bootstrap info by tenant code.
   */
  public Optional<TenantBootstrapInfo> findTenantInfoByCode(String tenantCode) {
    if (tenantCode == null || tenantCode.isBlank()) {
      return Optional.empty();
    }

    try {
      final String sql =
          "SELECT id, code, timezone, currency_code FROM tenant WHERE code = ? AND deleted_at IS NULL LIMIT 1";

      return jdbc.query(
          sql,
          ps -> ps.setString(1, tenantCode),
          rs -> {
            if (!rs.next()) return Optional.<TenantBootstrapInfo>empty();

            UUID id = (UUID) rs.getObject(1);
            String code = rs.getString(2);
            String tzRaw = rs.getString(3);
            String currencyRaw = rs.getString(4);

            var info =
                new TenantBootstrapInfo(
                    code,
                    TenantId.of(id),
                    safeZoneId(tzRaw, ZoneId.of("UTC")),
                    safeCurrency(currencyRaw, Currency.getInstance("USD")));

            return Optional.of(info);
          });

    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Find tenant bootstrap info by tenant ID.
   */
  public Optional<TenantBootstrapInfo> findTenantInfoById(TenantId tenantId) {
    if (tenantId == null) return Optional.empty();
    try {
      final String sql =
          "SELECT id, code, timezone, currency_code FROM tenant WHERE id = ? AND deleted_at IS NULL LIMIT 1";

      return jdbc.query(
          sql,
          ps -> ps.setObject(1, tenantId.value()),
          rs -> {
            if (!rs.next()) return Optional.<TenantBootstrapInfo>empty();

            UUID id = (UUID) rs.getObject(1);
            String code = rs.getString(2);
            String tzRaw = rs.getString(3);
            String currencyRaw = rs.getString(4);

            var info =
                new TenantBootstrapInfo(
                    code,
                    TenantId.of(id),
                    safeZoneId(tzRaw, ZoneId.of("UTC")),
                    safeCurrency(currencyRaw, Currency.getInstance("HTG")));

            return Optional.of(info);
          });

    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private static ZoneId safeZoneId(String raw, ZoneId fallback) {
    try {
      return (raw == null || raw.isBlank()) ? fallback : ZoneId.of(raw);
    } catch (Exception e) {
      return fallback;
    }
  }

  private static Currency safeCurrency(String raw, Currency fallback) {
    try {
      return (raw == null || raw.isBlank()) ? fallback : Currency.getInstance(raw);
    } catch (Exception e) {
      return fallback;
    }
  }
}
