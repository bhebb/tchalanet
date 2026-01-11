package com.tchalanet.server.core.draw.infra.persistence.repo;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DrawApplyJdbcRepository {

  private final JdbcTemplate jdbc;

  public DrawApplyJdbcRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public int attachResult(UUID drawId, UUID drawResultId, boolean force) {
    var sql =
        """
      update draw d
      set draw_result_id = ?,
          status = 'RESULTED',
          resulted_at = coalesce(d.resulted_at, now()),
          updated_at = now()
      where d.id = ?
        and d.deleted_at is null
        and d.locked = false
        and d.status = 'CLOSED'
        and (d.draw_result_id is null or ? = true)
      """;
    return jdbc.update(sql, drawResultId, drawId, force);
  }

  public int attachResultBySlot(
      UUID tenantId, LocalDate drawDate, UUID resultSlotId, UUID drawResultId, boolean force) {
    var sql =
        """
      update draw d
      set draw_result_id = ?, status='RESULTED', resulted_at=coalesce(d.resulted_at, now()), updated_at=now()
      from draw_channel dc
      where dc.id = d.draw_channel_id
        and d.tenant_id = ?
        and d.draw_date = ?
        and dc.result_slot_id = ?
        and d.deleted_at is null and dc.deleted_at is null
        and d.locked = false and d.status = 'CLOSED'
        and (d.draw_result_id is null or ? = true)
      """;

    return jdbc.update(sql, drawResultId, tenantId, drawDate, resultSlotId, force);
  }
}
