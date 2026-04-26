package com.tchalanet.server.catalog.tenant.internal.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository for tenant registry read access (bypasses RLS via rawDataSource).
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

  public org.springframework.data.domain.Page<TenantRegistryJpaEntity> findAll(
      org.springframework.data.domain.Pageable pageable) {

    final String countSql = "SELECT COUNT(*) FROM tenant";
    Long total = jdbc.queryForObject(countSql, Long.class);
    if (total == null) total = 0L;

    String orderByClause = buildOrderByClause(pageable.getSort());

    final String sql = SELECT_ALL_FIELDS + "FROM tenant " + orderByClause + " LIMIT ? OFFSET ?";

    List<TenantRegistryJpaEntity> content = jdbc.query(sql, ps -> {
      ps.setInt(1, pageable.getPageSize());
      ps.setLong(2, pageable.getOffset());
    }, (rs, rowNum) -> mapResultSetToEntity(rs));

    return new org.springframework.data.domain.PageImpl<>(
        content, pageable, total);
  }

  public List<TenantRegistryJpaEntity> findAllById(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) return Collections.emptyList();

    // Convert to list for JDBC params
    List<Object> params = new ArrayList<>(ids);
    String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
    String sql = SELECT_ALL_FIELDS + "FROM tenant WHERE id IN (" + placeholders + ") AND deleted_at IS NULL";

    return jdbc.query(sql, ps -> {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }, (rs, rowNum) -> mapResultSetToEntity(rs));
  }

  public long count() {
    return (long) countAll();
  }

  public int countAll() {
    try {
      Integer v = jdbc.queryForObject("SELECT COUNT(*) FROM tenant WHERE deleted_at IS NULL", Integer.class);
      return v == null ? 0 : v;
    } catch (Exception e) {
      return 0;
    }
  }

  public int countByStatus(String status) {
    if (status == null) return 0;
    try {
      Integer v = jdbc.queryForObject(
          "SELECT COUNT(*) FROM tenant WHERE status = ? AND deleted_at IS NULL",
          Integer.class,
          status);
      return v == null ? 0 : v;
    } catch (Exception e) {
      return 0;
    }
  }

  private String buildOrderByClause(org.springframework.data.domain.Sort sort) {
    if (sort.isUnsorted()) {
      return "ORDER BY created_at DESC";
    }

    var orders = new java.util.ArrayList<String>();
    for (var order : sort) {
      String column = mapSortFieldToColumn(order.getProperty());
      String direction = order.getDirection().isAscending() ? "ASC" : "DESC";
      orders.add(column + " " + direction);
    }

    return "ORDER BY " + String.join(", ", orders);
  }

  private String mapSortFieldToColumn(String field) {
    return switch (field) {
      case "createdAt" -> "created_at";
      case "code" -> "code";
      case "name" -> "name";
      case "status" -> "status";
      default -> "created_at";
    };
  }

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
    entity.setActiveThemeId((UUID) rs.getObject(9));
    entity.setDeletedAt(rs.getTimestamp(10) != null ? rs.getTimestamp(10).toInstant() : null);
    entity.setCreatedAt(rs.getTimestamp(11) != null ? rs.getTimestamp(11).toInstant() : null);
    entity.setUpdatedAt(rs.getTimestamp(12) != null ? rs.getTimestamp(12).toInstant() : null);
    return entity;
  }
}
