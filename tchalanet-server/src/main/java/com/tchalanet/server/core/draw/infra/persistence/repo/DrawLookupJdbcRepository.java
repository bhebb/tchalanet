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

  public UUID findDrawIdBySlotId(UUID tenantId, LocalDate drawDate, UUID resultSlotId) {
    var sql =
        """
      select d.id
      from draw d
      join draw_channel dc on dc.id = d.draw_channel_id
      where d.deleted_at is null
        and dc.deleted_at is null
        and d.tenant_id = ?
        and d.draw_date = ?
        and dc.result_slot_id = ?
      limit 1
    """;

    return jdbc.query(
        con -> {
          PreparedStatement ps = con.prepareStatement(sql);
          ps.setObject(1, tenantId);
          ps.setObject(2, drawDate);
          ps.setObject(3, resultSlotId);
          return ps;
        },
        rs -> rs.next() ? (UUID) rs.getObject("id") : null);
  }
}
