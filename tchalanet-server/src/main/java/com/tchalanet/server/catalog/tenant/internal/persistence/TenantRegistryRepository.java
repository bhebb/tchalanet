package com.tchalanet.server.catalog.tenant.internal.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository for tenant registry read access (bypasses RLS via rawDataSource).
 * Per DOMAIN_TENANT_CATALOG.md: provides direct JDBC access to tenant data.
 * Replaces TenantBootstrapLookup with unified catalog API.
 * Uses @Qualifier("rawDataSource") to bypass RLS policies.
 */
@Repository
public class TenantRegistryRepository {

  private static final String SELECT_ALL_FIELDS =
      "SELECT id, code, name, status, type, timezone, currency, address_id, active_theme_id, deleted_at, created_at, updated_at ";
  private static final String FROM_TENANT_NOT_DELETED = "FROM tenant WHERE deleted_at IS NULL";
  private static final String LIMIT_1 = " LIMIT 1";

  private final JdbcTemplate jdbc;

  public TenantRegistryRepository(@Qualifier("rawDataSource") DataSource rawDataSource) {
    this.jdbc = new JdbcTemplate(rawDataSource);
  }

  /**
   * Find tenant by code (case-insensitive), excluding soft-deleted.
   */
  public Optional<TenantRegistryJpaEntity> findByCodeIgnoreCase(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }

    try {
      final String sql = SELECT_ALL_FIELDS + FROM_TENANT_NOT_DELETED + " AND LOWER(code) = LOWER(?)" + LIMIT_1;

      return jdbc.query(
          sql,
          ps -> ps.setString(1, code),
          rs -> rs.next() ? Optional.of(mapResultSetToEntity(rs)) : Optional.empty());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Find tenant by ID, excluding soft-deleted.
   */
  public Optional<TenantRegistryJpaEntity> findByIdNotDeleted(UUID id) {
    if (id == null) {
      return Optional.empty();
    }

    try {
      final String sql = SELECT_ALL_FIELDS + FROM_TENANT_NOT_DELETED + " AND id = ?" + LIMIT_1;

      return jdbc.query(
          sql,
          ps -> ps.setObject(1, id),
          rs -> rs.next() ? Optional.of(mapResultSetToEntity(rs)) : Optional.empty());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * List all active (non-archived, non-deleted) tenant IDs.
   */
  public List<UUID> findAllActiveTenantIds() {
    try {
      final String sql = "SELECT id FROM tenant WHERE status != ? AND deleted_at IS NULL";

      return jdbc.query(sql, ps -> ps.setString(1, "ARCHIVED"), rs -> {
        var ids = new ArrayList<UUID>();
        while (rs.next()) {
          ids.add((UUID) rs.getObject(1));
        }
        return ids;
      });
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  /**
   * List ALL tenants (including soft-deleted) with pagination and sorting.
   * Used for admin listings - includes deleted tenants for audit/reactivation.
   * Per user request: return all tenants even soft-deleted for audit/reactivation.
   */
  public org.springframework.data.domain.Page<TenantRegistryJpaEntity> findAll(
      org.springframework.data.domain.Pageable pageable) {

    // Count total (ALL tenants, including soft-deleted)
    final String countSql = "SELECT COUNT(*) FROM tenant";
    Long total = jdbc.queryForObject(countSql, Long.class);
    if (total == null) total = 0L;

    // Build ORDER BY clause from Pageable sort
    String orderByClause = buildOrderByClause(pageable.getSort());

    // Fetch page with dynamic sorting
    final String sql = SELECT_ALL_FIELDS + "FROM tenant " + orderByClause + " LIMIT ? OFFSET ?";

    List<TenantRegistryJpaEntity> content = jdbc.query(sql, ps -> {
      ps.setInt(1, pageable.getPageSize());
      ps.setLong(2, pageable.getOffset());
    }, (rs, rowNum) -> mapResultSetToEntity(rs));

    return new org.springframework.data.domain.PageImpl<>(
        content, pageable, total);
  }

  /**
   * Build ORDER BY clause from Spring Sort.
   * Supports multiple sort fields.
   */
  private String buildOrderByClause(org.springframework.data.domain.Sort sort) {
    if (sort.isUnsorted()) {
      return "ORDER BY created_at DESC"; // Default sort
    }

    var orders = new java.util.ArrayList<String>();
    for (var order : sort) {
      String column = mapSortFieldToColumn(order.getProperty());
      String direction = order.getDirection().isAscending() ? "ASC" : "DESC";
      orders.add(column + " " + direction);
    }

    return "ORDER BY " + String.join(", ", orders);
  }

  /**
   * Map sort field names to actual DB column names.
   * Per @TchPaging allowedSort: createdAt, code, name, status
   */
  private String mapSortFieldToColumn(String field) {
    return switch (field) {
      case "createdAt" -> "created_at";
      case "code" -> "code";
      case "name" -> "name";
      case "status" -> "status";
      default -> "created_at"; // fallback
    };
  }

  /**
   * Helper: map ResultSet row to TenantRegistryJpaEntity.
   * Handles null values for timestamps and optional fields.
   */
  private TenantRegistryJpaEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
    var entity = new TenantRegistryJpaEntity();
    entity.setId((UUID) rs.getObject(1));
    entity.setCode(rs.getString(2));
    entity.setName(rs.getString(3));
    entity.setStatus(rs.getString(4));
    entity.setType(rs.getString(5));
    entity.setTimezone(rs.getString(6));
    entity.setCurrency(rs.getString(7));
    entity.setAddressId((UUID) rs.getObject(8));
    entity.setActiveThemeId(rs.getString(9));
    entity.setDeletedAt(rs.getTimestamp(10) != null ? rs.getTimestamp(10).toInstant() : null);
    entity.setCreatedAt(rs.getTimestamp(11) != null ? rs.getTimestamp(11).toInstant() : null);
    entity.setUpdatedAt(rs.getTimestamp(12) != null ? rs.getTimestamp(12).toInstant() : null);
    return entity;
  }
}
