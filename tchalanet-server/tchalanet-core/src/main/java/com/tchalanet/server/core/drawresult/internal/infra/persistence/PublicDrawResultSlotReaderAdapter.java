package com.tchalanet.server.core.drawresult.internal.infra.persistence;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.internal.application.port.out.PublicDrawResultSlotReaderPort;
import com.tchalanet.server.core.drawresult.internal.application.service.ResultSlotScheduleCalculator;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultHistoryRowView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotDetailsView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;

@Repository
@RequiredArgsConstructor
public class PublicDrawResultSlotReaderAdapter implements PublicDrawResultSlotReaderPort {

  private static final int MAX_HISTORY_PAGE_SIZE = 100;

  private final JdbcTemplate jdbc;
  private final ResultSlotCatalog resultSlotCatalog;
  private final ResultSlotScheduleCalculator scheduleCalculator;
  private final JsonUtils jsonUtils;

  @Override
  public List<PublicDrawResultSlotView> listPublicSlots(List<String> slotKeys, String provider) {
    var slots = findSlots(slotKeys, provider);
    var latestBySlot = findLatestBySlot(slots);

    return slots.stream()
        .map(slot -> toSlotView(slot, latestBySlot.get(slot.slotKey())))
        .toList();
  }

  @Override
  public List<PublicDrawResultSlotDetailsView> listPublicSlotDetails(
      List<String> slotKeys, String provider, int historyLimit) {
    var slots = findSlots(slotKeys, provider);
    var latestBySlot = findLatestBySlot(slots);
    var historyBySlot = findHistoryBySlot(slots, historyLimit);

    return slots.stream()
        .map(slot -> toSlotDetailsView(slot, latestBySlot.get(slot.slotKey()), historyBySlot))
        .toList();
  }

  @Override
  public TchPage<PublicDrawResultHistoryRowView> searchPublicHistory(
      List<String> slotKeys,
      String provider,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    int page = Math.max(0, pageable.getPageNumber());
    int size = Math.min(Math.max(1, pageable.getPageSize()), MAX_HISTORY_PAGE_SIZE);
    int offset = page * size;

    var params = new ArrayList<Object>();
    var where = publicHistoryWhere(slotKeys, provider, from, to, params);
    var rows =
        jdbc.query(
            publicHistorySelect() + where + " order by dr.occurred_at desc limit ? offset ?",
            this::mapHistoryRow,
            append(params, size, offset));

    long total = countPublicHistory(where, params);
    int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
    boolean last = totalPages == 0 || page >= totalPages - 1;

    return new TchPage<>(rows, page, size, total, totalPages, last, !last, page > 0);
  }

  private List<SlotRow> findSlots(List<String> slotKeys, String provider) {
    var normalizedSlotKeys = slotKeys == null ? List.<String>of() : slotKeys;
    var normalizedProvider = provider == null ? null : provider.trim().toUpperCase();

    return resultSlotCatalog.listActive().stream()
        .filter(slot -> normalizedSlotKeys.isEmpty() || normalizedSlotKeys.contains(slot.slotKey().toUpperCase()))
        .filter(slot -> normalizedProvider == null || normalizedProvider.equals(slot.provider().toUpperCase()))
        .map(this::toSlotRow)
        .toList();
  }

  private Map<String, ResultRow> findLatestBySlot(List<SlotRow> slots) {
    if (slots.isEmpty()) {
      return Map.of();
    }

    var slotKeys = slots.stream().map(SlotRow::slotKey).toList();
    var params = new ArrayList<Object>();
    var sql =
        new StringBuilder(
            """
            select *
            from (
              select
                rs.slot_key,
                rs.provider,
                rs.label_key,
                rs.timezone,
                rs.draw_time,
                dr.occurred_at,
                dr.status,
                dr.quality,
                dr.haiti_result,
                dr.source_result,
                row_number() over (partition by rs.slot_key order by dr.occurred_at desc) as rn
              from result_slot rs
              join draw_result dr on dr.result_slot_id = rs.id
              where rs.deleted_at is null
                and rs.active = true
                and dr.deleted_at is null
            """);
    appendSlotKeyFilter(sql, params, slotKeys);
    sql.append(") latest where rn = 1");

    var rows = jdbc.query(sql.toString(), this::mapResultRow, params.toArray());
    var bySlot = new LinkedHashMap<String, ResultRow>();
    rows.forEach(row -> bySlot.put(row.slotKey(), row));
    return bySlot;
  }

  private Map<String, List<PublicDrawResultHistoryRowView>> findHistoryBySlot(
      List<SlotRow> slots, int historyLimit) {
    if (slots.isEmpty() || historyLimit <= 0) {
      return Map.of();
    }

    var slotKeys = slots.stream().map(SlotRow::slotKey).toList();
    var params = new ArrayList<Object>();
    var sql =
        new StringBuilder(
            """
            select *
            from (
              select
                rs.slot_key,
                rs.provider,
                rs.label_key,
                rs.timezone,
                rs.draw_time,
                dr.occurred_at,
                dr.status,
                dr.quality,
                dr.haiti_result,
                dr.source_result,
                row_number() over (partition by rs.slot_key order by dr.occurred_at desc) as rn
              from result_slot rs
              join draw_result dr on dr.result_slot_id = rs.id
              where rs.deleted_at is null
                and rs.active = true
                and dr.deleted_at is null
            """);
    appendSlotKeyFilter(sql, params, slotKeys);
    sql.append(") history where rn <= ? order by slot_key asc, occurred_at desc");
    params.add(historyLimit);

    var rows = jdbc.query(sql.toString(), this::mapHistoryRow, params.toArray());
    var bySlot = new LinkedHashMap<String, List<PublicDrawResultHistoryRowView>>();
    rows.forEach(row -> bySlot.computeIfAbsent(row.slotKey(), ignored -> new ArrayList<>()).add(row));
    return bySlot;
  }

  private PublicDrawResultSlotView toSlotView(SlotRow slot, ResultRow latest) {
    return new PublicDrawResultSlotView(
        slot.slotKey(),
        slot.provider(),
        label(slot),
        slot.timezone(),
        slot.drawTime(),
        slot.active(),
        scheduleCalculator.calculateNextResultTime(slot.drawTime(), slot.timezone(), slot.active()),
        latest == null ? null : toResultView(latest),
        List.of());
  }

  private PublicDrawResultSlotDetailsView toSlotDetailsView(
      SlotRow slot,
      ResultRow latest,
      Map<String, List<PublicDrawResultHistoryRowView>> historyBySlot) {
    return new PublicDrawResultSlotDetailsView(
        slot.slotKey(),
        slot.provider(),
        label(slot),
        slot.timezone(),
        slot.drawTime(),
        slot.active(),
        scheduleCalculator.calculateNextResultTime(slot.drawTime(), slot.timezone(), slot.active()),
        latest == null ? null : toResultView(latest),
        historyBySlot.getOrDefault(slot.slotKey(), List.of()));
  }

  private PublicDrawResultView toResultView(ResultRow row) {
    return new PublicDrawResultView(
        resultDate(row.occurredAt()),
        row.occurredAt(),
        row.status(),
        row.quality(),
        row.haiti(),
        row.source());
  }

  private StringBuilder publicHistoryWhere(
      List<String> slotKeys,
      String provider,
      LocalDate from,
      LocalDate to,
      List<Object> params) {
    var where =
        new StringBuilder(
            """
            where rs.deleted_at is null
              and rs.active = true
              and dr.deleted_at is null
            """);

    appendSlotFilters(where, params, slotKeys, provider);

    if (from != null) {
      where.append(" and dr.occurred_at >= ?");
      params.add(Timestamp.from(from.atStartOfDay(ZoneOffset.UTC).toInstant()));
    }

    if (to != null) {
      where.append(" and dr.occurred_at < ?");
      params.add(Timestamp.from(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
    }

    return where;
  }

  private String publicHistorySelect() {
    return """
        select
          rs.slot_key,
          rs.provider,
          rs.label_key,
          rs.timezone,
          rs.draw_time,
          dr.occurred_at,
          dr.status,
          dr.quality,
          dr.haiti_result,
          dr.source_result
        from result_slot rs
        join draw_result dr on dr.result_slot_id = rs.id
        """;
  }

  private long countPublicHistory(StringBuilder where, List<Object> params) {
    var count =
        jdbc.queryForObject(
            """
            select count(1)
            from result_slot rs
            join draw_result dr on dr.result_slot_id = rs.id
            """
                + where,
            params.toArray(),
            Long.class);
    return count == null ? 0L : count;
  }

  private void appendSlotFilters(
      StringBuilder sql, List<Object> params, List<String> slotKeys, String provider) {
    appendSlotKeyFilter(sql, params, slotKeys);

    if (provider != null && !provider.isBlank()) {
      sql.append(" and upper(rs.provider) = ?");
      params.add(provider);
    }
  }

  private void appendSlotKeyFilter(StringBuilder sql, List<Object> params, List<String> slotKeys) {
    if (slotKeys == null || slotKeys.isEmpty()) {
      return;
    }

    sql.append(" and upper(rs.slot_key) in (");
    for (int i = 0; i < slotKeys.size(); i++) {
      sql.append(i == 0 ? "?" : ",?");
      params.add(slotKeys.get(i));
    }
    sql.append(")");
  }

  private Object[] append(List<Object> params, Object... values) {
    var copy = new ArrayList<>(params);
    copy.addAll(List.of(values));
    return copy.toArray();
  }

  private SlotRow toSlotRow(ResultSlotView slot) {
    return new SlotRow(
        slot.slotKey(),
        slot.provider(),
        slot.labelKey(),
        slot.timezone().getId(),
        slot.drawTime(),
        slot.active());
  }

  private ResultRow mapResultRow(ResultSet rs, int rowNum) throws SQLException {
    return new ResultRow(
        rs.getString("slot_key"),
        instant(rs, "occurred_at"),
        rs.getString("status"),
        rs.getString("quality"),
        json(rs.getObject("haiti_result")),
        json(rs.getObject("source_result")));
  }

  private PublicDrawResultHistoryRowView mapHistoryRow(ResultSet rs, int rowNum)
      throws SQLException {
    var occurredAt = instant(rs, "occurred_at");
    return new PublicDrawResultHistoryRowView(
        rs.getString("slot_key"),
        rs.getString("provider"),
        label(rs.getString("label_key"), rs.getString("slot_key")),
        rs.getString("timezone"),
        rs.getObject("draw_time", LocalTime.class),
        resultDate(occurredAt),
        occurredAt,
        rs.getString("status"),
        rs.getString("quality"),
        json(rs.getObject("haiti_result")),
        json(rs.getObject("source_result")));
  }

  private JsonNode json(Object value) {
    return value == null ? jsonUtils.emptyObject() : jsonUtils.toJsonNode(value.toString());
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    var timestamp = rs.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private LocalDate resultDate(Instant occurredAt) {
    return occurredAt == null ? null : occurredAt.atZone(ZoneOffset.UTC).toLocalDate();
  }

  private String label(SlotRow slot) {
    return label(slot.labelKey(), slot.slotKey());
  }

  private String label(String labelKey, String slotKey) {
    return labelKey == null || labelKey.isBlank() ? slotKey : labelKey;
  }

  private record SlotRow(
      String slotKey,
      String provider,
      String labelKey,
      String timezone,
      LocalTime drawTime,
      boolean active) {}

  private record ResultRow(
      String slotKey,
      Instant occurredAt,
      String status,
      String quality,
      JsonNode haiti,
      JsonNode source) {}
}
