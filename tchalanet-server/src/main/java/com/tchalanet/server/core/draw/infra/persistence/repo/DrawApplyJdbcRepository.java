package com.tchalanet.server.core.draw.infra.persistence.repo;

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
}
