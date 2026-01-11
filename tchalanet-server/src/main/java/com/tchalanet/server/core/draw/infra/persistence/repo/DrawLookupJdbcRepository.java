package com.tchalanet.server.core.draw.infra.persistence.repo;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DrawLookupJdbcRepository {

  private final JdbcTemplate jdbc;

  public DrawLookupJdbcRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public UUID findDrawId(UUID tenantId, LocalDate drawDate, String slotKey) {
    // Adjust column names to your real schema if different
    var sql =
        """
      select d.id
      from draw d
      where d.deleted_at is null
        and d.tenant_id = ?
        and d.draw_date = ?
        and d.slot_key = ?
      limit 1
    """;

    try {
      return jdbc.query(
          con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, tenantId);
            ps.setObject(2, drawDate);
            ps.setString(3, slotKey);
            return ps;
          },
          rs -> rs.next() ? (UUID) rs.getObject("id") : null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to lookup draw id", e);
    }
  }

  public UUID findDrawIdBySlotId(UUID tenantId, LocalDate drawDate, UUID resultSlotId) {
    var sql =
        """
      select d.id
      from draw d
      where d.deleted_at is null
        and d.tenant_id = ?
        and d.draw_date = ?
        and d.result_slot_id = ?
      limit 1
    """;

    try {
      return jdbc.query(
          con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, tenantId);
            ps.setObject(2, drawDate);
            ps.setObject(3, resultSlotId);
            return ps;
          },
          rs -> rs.next() ? (UUID) rs.getObject("id") : null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to lookup draw id by slot id", e);
    }
  }
}
