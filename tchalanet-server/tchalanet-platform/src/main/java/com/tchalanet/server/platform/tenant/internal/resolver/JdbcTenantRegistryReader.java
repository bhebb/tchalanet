package com.tchalanet.server.platform.tenant.internal.resolver;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Uses rawDataSource intentionally because tenant registry is needed before
 * tenant RLS can be bound. This reader must remain read-only and must only
 * be used by platform/bootstrap context resolution code.
 */
@Component
class JdbcTenantRegistryReader implements TenantRegistryReader {

    private static final String SELECT =
        "SELECT id, code, name, status, type, timezone, currency, " +
            "default_language, default_locale, address_id, active_theme_id, " +
            "default_commission_rate " +
            "FROM tenant WHERE deleted_at IS NULL";

    private final JdbcTemplate jdbc;

    JdbcTenantRegistryReader(@Qualifier("rawDataSource") DataSource rawDataSource) {
        this.jdbc = new JdbcTemplate(rawDataSource);
    }

    @Override
    public Optional<TenantBootstrapRow> findByCode(String codeLower) {
        if (codeLower == null || codeLower.isBlank()) return Optional.empty();
        try {
            return jdbc.query(
                SELECT + " AND LOWER(code) = LOWER(?) LIMIT 1",
                ps -> ps.setString(1, codeLower),
                rs -> rs.next() ? Optional.of(map(rs)) : Optional.empty());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TenantBootstrapRow> findById(UUID tenantId) {
        if (tenantId == null) return Optional.empty();
        try {
            return jdbc.query(
                SELECT + " AND id = ? LIMIT 1",
                ps -> ps.setObject(1, tenantId),
                rs -> rs.next() ? Optional.of(map(rs)) : Optional.empty());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<UUID> listActiveTenantIds() {
        try {
            return jdbc.query(
                "SELECT id FROM tenant WHERE status != 'ARCHIVED' AND deleted_at IS NULL",
                rs -> {
                    var ids = new ArrayList<UUID>();
                    while (rs.next()) ids.add((UUID) rs.getObject(1));
                    return ids;
                });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<TenantBootstrapRow> listAll(int limit, int offset, String orderBy) {
        try {
            return jdbc.query(
                SELECT + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
                ps -> {
                    ps.setInt(1, limit);
                    ps.setInt(2, offset);
                },
                (rs, rowNum) -> map(rs));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public long countAll() {
        try {
            Long v = jdbc.queryForObject("SELECT COUNT(*) FROM tenant WHERE deleted_at IS NULL", Long.class);
            return v == null ? 0L : v;
        } catch (Exception e) {
            return 0L;
        }
    }

    private TenantBootstrapRow map(ResultSet rs) throws SQLException {
        return new TenantBootstrapRow(
            (UUID) rs.getObject(1),
            rs.getString(2),
            rs.getString(3),
            rs.getString(4),
            rs.getString(5),
            rs.getString(6),
            rs.getString(7),
            rs.getString(8),
            rs.getString(9),
            (UUID) rs.getObject(10),
            (UUID) rs.getObject(11),
            rs.getBigDecimal(12));
    }
}
