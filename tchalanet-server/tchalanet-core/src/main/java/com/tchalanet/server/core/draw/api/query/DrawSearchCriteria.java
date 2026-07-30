package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Search criteria for draw summaries.
 *
 * <p>{@code from}/{@code to} bound the business date (day granularity). {@code scheduledBefore}/
 * {@code scheduledAfter} bound the actual draw instant — a draw at 12:29 today is "past" at 14:00
 * but still shares today's business date, so the two are not interchangeable.
 */
public record DrawSearchCriteria(
    ResultSlotId resultSlotId,
    DrawStatus status,
    LocalDate from,
    LocalDate to,
    Integer limitPerChannel,
    Integer lookaheadHours,
    List<String> resultSlotKeys,
    Instant scheduledBefore,
    Instant scheduledAfter) {

  public static DrawSearchCriteria of(
      ResultSlotId resultSlotId, String status, LocalDate from, LocalDate to) {

    return of(resultSlotId, status, from, to, null, null);
  }

  public static DrawSearchCriteria of(
      ResultSlotId resultSlotId,
      String status,
      LocalDate from,
      LocalDate to,
      Instant scheduledBefore,
      Instant scheduledAfter) {

    return new DrawSearchCriteria(
        resultSlotId,
        parseStatus(status),
        from,
        to,
        null,
        null,
        null,
        scheduledBefore,
        scheduledAfter);
  }

  public static DrawSearchCriteria today(ResultSlotId resultSlotId, LocalDate today) {
    return new DrawSearchCriteria(resultSlotId, null, today, today, null, null, null, null, null);
  }

  public static DrawSearchCriteria upcoming(ResultSlotId resultSlotId, LocalDate today, int days) {

    return new DrawSearchCriteria(
        resultSlotId, null, today, today.plusDays(days), null, null, null, null, null);
  }

  public static DrawSearchCriteria forNext(
      ResultSlotId resultSlotId, int lookaheadHours, int limitPerChannel) {

    return new DrawSearchCriteria(
        resultSlotId, null, null, null, limitPerChannel, lookaheadHours, null, null, null);
  }

  public static DrawSearchCriteria forNextOpen(
      ResultSlotId resultSlotId, int lookaheadHours, int limitPerChannel) {

    return new DrawSearchCriteria(
        resultSlotId,
        DrawStatus.OPEN,
        null,
        null,
        limitPerChannel,
        lookaheadHours,
        null,
        null,
        null);
  }

  public static DrawSearchCriteria forLatestWithResults(List<String> resultSlotKeys) {
    return new DrawSearchCriteria(
        null, null, null, null, null, null, normalizeKeys(resultSlotKeys), null, null);
  }

  public static DrawSearchCriteria forResults(
      List<String> resultSlotKeys, LocalDate from, LocalDate to) {

    return new DrawSearchCriteria(
        null, DrawStatus.RESULTED, from, to, null, null, normalizeKeys(resultSlotKeys), null, null);
  }

  public DrawSearchCriteria withResultSlotKey(String resultSlotKey) {
    return new DrawSearchCriteria(
        resultSlotId,
        status,
        from,
        to,
        limitPerChannel,
        lookaheadHours,
        normalizeKeys(resultSlotKey == null ? null : List.of(resultSlotKey)),
        scheduledBefore,
        scheduledAfter);
  }

  private static DrawStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }

    return DrawStatus.valueOf(status.trim().toUpperCase());
  }

  private static List<String> normalizeKeys(List<String> keys) {
    if (keys == null || keys.isEmpty()) {
      return List.of();
    }

    var normalized =
        keys.stream()
            .filter(k -> k != null && !k.isBlank())
            .map(k -> k.trim().toUpperCase())
            .distinct()
            .toList();

    return normalized.isEmpty() ? null : normalized;
  }
}
