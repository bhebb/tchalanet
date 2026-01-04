package com.tchalanet.server.core.draw.infra.persistence.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ApplyCandidateDrawJdbcRepository {

  private final JdbcTemplate jdbc;

  public ApplyCandidateDrawJdbcRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<UUID> findClosedDrawIds(LocalDate drawDate, List<String> channelCodes, int maxDraws) {
    var in = String.join(",", channelCodes.stream().map(c -> "?").toList());
    var sql =
        """
        select d.id
        from draw d
        join draw_channel dc on dc.id = d.draw_channel_id
        where d.deleted_at is null
          and dc.deleted_at is null
          and dc.active = true
          and d.locked = false
          and d.status = 'CLOSED'
          and d.draw_date = ?
          and dc.code in ("""
            + in
            + """
          )
        order by d.scheduled_at asc
        limit ?
        """;

    var args = new java.util.ArrayList<>();
    args.add(drawDate);
    args.addAll(channelCodes);
    args.add(maxDraws);

    return jdbc.query(sql, (rs, i) -> (UUID) rs.getObject(1), args.toArray());
  }

  public DrawKey findDrawKey(UUID drawId) {
    var sql =
        """
      select dc.code as channelCode, d.draw_date as drawDate
      from draw d
      join draw_channel dc on dc.id = d.draw_channel_id
      where d.id = ? and d.deleted_at is null
      """;
    return jdbc.queryForObject(
        sql,
        (rs, i) ->
            new DrawKey(rs.getString("channelCode"), rs.getObject("drawDate", LocalDate.class)),
        drawId);
  }

  public record DrawKey(String channelCode, LocalDate drawDate) {}
}
