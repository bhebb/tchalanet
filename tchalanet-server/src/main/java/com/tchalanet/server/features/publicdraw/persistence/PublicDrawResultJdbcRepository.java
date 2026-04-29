package com.tchalanet.server.features.publicdraw.persistence;

import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.features.publicdraw.persistence.PublicDrawResultRow;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PublicDrawResultJdbcRepository implements PublicDrawResultRepository {

  private final JdbcTemplate jdbc;

  // ---- helpers -------------------------------------------------------------

  private static Date sqlDate(LocalDate d) {
    return d == null ? null : Date.valueOf(d);
  }

  private static String normalizeSortProperty(String raw) {
    if (raw == null) return "occurredAt";
    return switch (raw) {
      case "occurredAt" -> "occurredAt";
      case "drawDate" -> "drawDate";
      case "slotKey" -> "slotKey";
      case "provider" -> "provider";
      default -> "occurredAt";
    };
  }

  private static String toSqlSortColumn(String property) {
    // Must match SELECT aliases below
    return switch (property) {
      case "slotKey" -> "slotKey";
      case "provider" -> "provider";
      case "drawDate" -> "drawDate";
      case "occurredAt" -> "occurredAt";
      default -> "occurredAt";
    };
  }

  // ---- pageable search (interface) ----------------------------------------
  @Override
  public TchPage<PublicDrawResultRow> search(
      String slotKey, String provider, LocalDate from, LocalDate to, Pageable pageable) {
    int size = Math.max(1, Math.min(100, pageable == null ? 20 : pageable.getPageSize()));
    int page = Math.max(0, pageable == null ? 0 : pageable.getPageNumber());
    int offset = page * size;
    Sort sort = (pageable == null) ? Sort.unsorted() : pageable.getSort();

    long total = count(slotKey, provider, from, to);
    List<PublicDrawResultRow> rows = search(slotKey, provider, from, to, size, offset, sort);

    int totalPages = size == 0 ? 0 : (int) ((total + size - 1) / size);
    boolean last = page >= (totalPages - 1);
    boolean hasNext = page < (totalPages - 1);
    boolean hasPrev = page > 0;

    return TchPage.of(rows, page, size, total, totalPages, last, hasNext, hasPrev);
  }

  // ---- count ---------------------------------------------------------------

  public long count(String slotKey, String provider, LocalDate from, LocalDate to) {
    var sb =
        new StringBuilder(
            """
            select count(1)
            from draw_result dr
            join result_slot rs on rs.id = dr.result_slot_id
            where dr.deleted_at is null
              and rs.deleted_at is null
              and rs.active = true
              and dr.status = 'VALID'
            """);

    var params = new ArrayList<Object>();

    if (slotKey != null && !slotKey.isBlank()) {
      sb.append(" and rs.slot_key = ?");
      params.add(slotKey);
    }
    if (provider != null && !provider.isBlank()) {
      sb.append(" and rs.provider = ?");
      params.add(provider);
    }
    if (from != null) {
      sb.append(" and (dr.occurred_at at time zone rs.timezone)::date >= ?");
      params.add(sqlDate(from));
    }
    if (to != null) {
      sb.append(" and (dr.occurred_at at time zone rs.timezone)::date <= ?");
      params.add(sqlDate(to));
    }

    Long c = jdbc.queryForObject(sb.toString(), params.toArray(), Long.class);
    return c == null ? 0L : c;
  }

  // ---- search page ---------------------------------------------------------

  public List<PublicDrawResultRow> search(
      String slotKey,
      String provider,
      LocalDate from,
      LocalDate to,
      int limit,
      int offset,
      Sort sort) {

    // whitelist sort
    String sortProp = "occurredAt";
    Sort.Direction dir = Sort.Direction.DESC;

    if (sort != null && sort.isSorted()) {
      var first = sort.iterator().next();
      sortProp = normalizeSortProperty(first.getProperty());
      dir = first.getDirection() == null ? Sort.Direction.DESC : first.getDirection();
    }

    String orderBy = toSqlSortColumn(sortProp) + " " + (dir.isAscending() ? "asc" : "desc");

    var sb =
        new StringBuilder(
            """
            select
              rs.slot_key as slotKey,
              rs.provider as provider,
              rs.timezone as slotTimezone,
              rs.draw_time as slotDrawTime,
              rs.days_of_week as daysOfWeek,

              (dr.occurred_at at time zone rs.timezone)::date as drawDate,
              dr.occurred_at as occurredAt,

              dr.haiti_result::text as haitiResultJson,
              dr.source_result::text as sourceResultJson,

              dr.status as status,
              dr.quality as quality,
              dr.source as source
            from draw_result dr
            join result_slot rs on rs.id = dr.result_slot_id
            where dr.deleted_at is null
              and rs.deleted_at is null
              and rs.active = true
              and dr.status = 'VALID'
            """);

    var params = new ArrayList<Object>();

    if (slotKey != null && !slotKey.isBlank()) {
      sb.append(" and rs.slot_key = ?");
      params.add(slotKey);
    }
    if (provider != null && !provider.isBlank()) {
      sb.append(" and rs.provider = ?");
      params.add(provider);
    }
    if (from != null) {
      sb.append(" and (dr.occurred_at at time zone rs.timezone)::date >= ?");
      params.add(sqlDate(from));
    }
    if (to != null) {
      sb.append(" and (dr.occurred_at at time zone rs.timezone)::date <= ?");
      params.add(sqlDate(to));
    }

    sb.append(" order by ").append(orderBy);
    sb.append(" limit ? offset ?");
    params.add(Math.max(1, limit));
    params.add(Math.max(0, offset));

    return jdbc.query(
        sb.toString(),
        (rs, rowNum) -> {
          String sk = rs.getString("slotKey");
          String prov = rs.getString("provider");
          String tz = rs.getString("slotTimezone");

          var lt = rs.getTime("slotDrawTime");
          LocalTime drawTime = (lt == null) ? null : lt.toLocalTime();

          LocalDate drawDate = null;
          var dd = rs.getDate("drawDate");
          if (dd != null) drawDate = dd.toLocalDate();

          Instant occurredAt = null;
          var ts = rs.getTimestamp("occurredAt");
          if (ts != null) occurredAt = ts.toInstant();

          String haitiJson = rs.getString("haitiResultJson");
          String sourceJson = rs.getString("sourceResultJson");

          String status = rs.getString("status");
          String quality = rs.getString("quality");
          String source = rs.getString("source");
          String dow = rs.getString("daysOfWeek");

          return new PublicDrawResultRowImpl(
              sk,
              prov,
              tz,
              drawTime,
              dow,
              drawDate,
              occurredAt,
              sourceJson,
              haitiJson,
              status,
              quality,
              source);
        },
        params.toArray());
  }

  // ---- findOne -------------------------------------------------------------

  @Override
  public Optional<PublicDrawResultRow> findOne(String slotKey, LocalDate drawDate) {
    var sql =
        """
        select
          rs.slot_key as slotKey,
          rs.provider as provider,
          rs.timezone as slotTimezone,
          rs.draw_time as slotDrawTime,
          rs.days_of_week as daysOfWeek,

          (dr.occurred_at at time zone rs.timezone)::date as drawDate,
          dr.occurred_at as occurredAt,

          dr.haiti_result::text as haitiResultJson,
          dr.source_result::text as sourceResultJson,

          dr.status as status,
          dr.quality as quality,
          dr.source as source
        from draw_result dr
        join result_slot rs on rs.id = dr.result_slot_id
        where dr.deleted_at is null
          and rs.deleted_at is null
          and rs.active = true
          and dr.status = 'VALID'
          and rs.slot_key = ?
          and (dr.occurred_at at time zone rs.timezone)::date = ?
        order by dr.occurred_at desc
        limit 1
        """;

    var rows =
        jdbc.query(
            sql,
            (rs, rowNum) -> {
              String sk = rs.getString("slotKey");
              String prov = rs.getString("provider");
              String tz = rs.getString("slotTimezone");

              var lt = rs.getTime("slotDrawTime");
              LocalTime drawTime = (lt == null) ? null : lt.toLocalTime();

              LocalDate dd =
                  rs.getDate("drawDate") == null ? null : rs.getDate("drawDate").toLocalDate();
              Instant occ =
                  rs.getTimestamp("occurredAt") == null
                      ? null
                      : rs.getTimestamp("occurredAt").toInstant();

              String haitiJson = rs.getString("haitiResultJson");
              String sourceJson = rs.getString("sourceResultJson");

              String status = rs.getString("status");
              String quality = rs.getString("quality");
              String source = rs.getString("source");
              String dow = rs.getString("daysOfWeek");

              return new PublicDrawResultRowImpl(
                  sk,
                  prov,
                  tz,
                  drawTime,
                  dow,
                  dd,
                  occ,
                  sourceJson,
                  haitiJson,
                  status,
                  quality,
                  source);
            },
            slotKey,
            sqlDate(drawDate));

    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  // ---- latest --------------------------------------------------------------

  public List<PublicDrawResultRow> latest(int limitPerSlot) {
    int limit = Math.max(1, Math.min(10, limitPerSlot));

    var sql =
        """
        with ranked as (
          select
            rs.slot_key as slotKey,
            rs.provider as provider,
            rs.timezone as slotTimezone,
            rs.draw_time as slotDrawTime,
            rs.days_of_week as daysOfWeek,

            (dr.occurred_at at time zone rs.timezone)::date as drawDate,
            dr.occurred_at as occurredAt,

            dr.haiti_result::text as haitiResultJson,
            dr.source_result::text as sourceResultJson,

            dr.status as status,
            dr.quality as quality,
            dr.source as source,

            row_number() over (partition by rs.slot_key order by dr.occurred_at desc) as rn
          from draw_result dr
          join result_slot rs on rs.id = dr.result_slot_id
          where dr.deleted_at is null
            and rs.deleted_at is null
            and rs.active = true
            and dr.status = 'VALID'
        )
        select *
        from ranked
        where rn <= ?
        order by slotKey, occurredAt desc
        """;

    return jdbc.query(
        sql,
        (rs, rowNum) -> {
          String sk = rs.getString("slotKey");
          String prov = rs.getString("provider");
          String tz = rs.getString("slotTimezone");

          var lt = rs.getTime("slotDrawTime");
          LocalTime drawTime = (lt == null) ? null : lt.toLocalTime();

          LocalDate dd =
              rs.getDate("drawDate") == null ? null : rs.getDate("drawDate").toLocalDate();
          Instant occ =
              rs.getTimestamp("occurredAt") == null
                  ? null
                  : rs.getTimestamp("occurredAt").toInstant();

          String haitiJson = rs.getString("haitiResultJson");
          String sourceJson = rs.getString("sourceResultJson");

          String status = rs.getString("status");
          String quality = rs.getString("quality");
          String source = rs.getString("source");
          String dow = rs.getString("daysOfWeek");

          return new PublicDrawResultRowImpl(
              sk, prov, tz, drawTime, dow, dd, occ, sourceJson, haitiJson, status, quality, source);
        },
        limit);
  }

  // ---- Row -------------------------------------------------------------

  private record PublicDrawResultRowImpl(
      String slotKey,
      String provider,
      String slotTimezone,
      LocalTime slotDrawTime,
      String daysOfWeek,
      LocalDate drawDate,
      Instant occurredAt,
      String sourceResultJson,
      String haitiResultJson,
      String status,
      String quality,
      String source)
      implements PublicDrawResultRow {

    @Override
    public String getSlotKey() {
      return slotKey;
    }

    @Override
    public String getProvider() {
      return provider;
    }

    @Override
    public String getSlotTimezone() {
      return slotTimezone;
    }

    @Override
    public LocalTime getSlotDrawTime() {
      return slotDrawTime;
    }

    public String getDaysOfWeek() {
      return daysOfWeek;
    } // si tu veux l'ajouter à l'interface

    @Override
    public LocalDate getDrawDate() {
      return drawDate;
    }

    @Override
    public Instant getOccurredAt() {
      return occurredAt;
    }

    @Override
    public String getSourceResultJson() {
      return sourceResultJson;
    }

    @Override
    public String getHaitiResultJson() {
      return haitiResultJson;
    }

    @Override
    public DrawResultStatus getStatus() {
      // si ton interface PublicDrawResultRow attend un enum, convertis ici
      return status == null
          ? null
          : DrawResultStatus.valueOf(status);
    }

    @Override
    public com.tchalanet.server.common.types.enums.ResultQuality getQuality() {
      return quality == null
          ? null
          : com.tchalanet.server.common.types.enums.ResultQuality.valueOf(quality);
    }

    @Override
    public String getSource() {
      return source;
    }
  }
}
