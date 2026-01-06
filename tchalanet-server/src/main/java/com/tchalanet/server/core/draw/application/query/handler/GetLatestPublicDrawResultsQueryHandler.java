package com.tchalanet.server.core.draw.application.query.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.util.HolidayUtils;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.draw.application.port.out.PublicDrawResultPort;
import com.tchalanet.server.core.draw.application.print.DrawChannelLabelResolver;
import com.tchalanet.server.core.draw.application.query.model.GetLatestPublicDrawResultsQuery;
import com.tchalanet.server.core.draw.infra.persistence.PublicDrawResultRow;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultItemResponse;
import com.tchalanet.server.core.draw.infra.web.model.PublicLatestDrawResultsResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetLatestPublicDrawResultsQueryHandler
    implements QueryHandler<
        GetLatestPublicDrawResultsQuery, List<PublicLatestDrawResultsResponse>> {

  private final PublicDrawResultPort port;
  private final JsonUtils jsonUtils;
  private final DrawChannelLabelResolver channelLabelResolver;
  private final HolidayUtils holidayUtils;
  private final TimeProvider time; // ✅ use injected clock for testability

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  @Override
  public List<PublicLatestDrawResultsResponse> handle(GetLatestPublicDrawResultsQuery query) {
    List<PublicDrawResultRow> rows = port.latest(query.limitPerChannel());

    return rows.stream()
        .collect(Collectors.groupingBy(PublicDrawResultRow::getChannelCode))
        .values()
        .stream()
        .map(this::mapToLatestResponse)
        .toList();
  }

  private PublicLatestDrawResultsResponse mapToLatestResponse(List<PublicDrawResultRow> rows) {
    var first = rows.getFirst();

    List<PublicDrawResultItemResponse> results =
        rows.stream().map(this::mapToItemResponse).toList();

    String channelLabel =
        channelLabelResolver.resolve(
            first.getChannelName(), first.getChannelDrawTime(), java.util.Locale.getDefault());

    // ---- NEXT DRAW (MVP SIMPLE) --------------------------------------------
    // Rules:
    // - Compute "today" in the channel timezone.
    // - If latest result is stale (drawDate < today - 1 day) => hide next draw.
    // - If today is a holiday for the provider/channel => hide next draw.
    // - Else compute candidate with drawTime (today at draw time, else tomorrow).
    Instant nextScheduledAt = null;
    String nextDrawLabel = null;
    Boolean nextIsOpen = null;
    Boolean nextIsClosingSoon = null;

    LocalTime drawTime = first.getChannelDrawTime();
    ZoneId zone =
        first.getChannelTimezone() != null ? first.getChannelTimezone() : ZoneId.of("UTC");

    if (drawTime != null) {
      ZonedDateTime now = time.now(zone);
      LocalDate today = now.toLocalDate();

      // 1) stale gate (your current rule: hide if older than 1 day)
      if (first.getDrawDate() != null && first.getDrawDate().isBefore(today.minusDays(1))) {
        return buildResponse(
            first, results, nextScheduledAt, nextDrawLabel, nextIsOpen, nextIsClosingSoon);
      }

      // 2) holiday gate (MM-dd config handled inside HolidayUtils)
      if (holidayUtils.isHolidayForChannel(first.getChannelCode(), today)) {
        return buildResponse(
            first, results, nextScheduledAt, nextDrawLabel, nextIsOpen, nextIsClosingSoon);
      }

      // 3) compute candidate
      ZonedDateTime candidate = ZonedDateTime.of(today, drawTime, zone);
      if (!candidate.isAfter(now)) {
        candidate = candidate.plusDays(1);
      }

      // Optional (no loops): hide if candidate date is also a holiday
      if (holidayUtils.isHolidayForChannel(first.getChannelCode(), candidate.toLocalDate())) {
        return buildResponse(
            first, results, nextScheduledAt, nextDrawLabel, nextIsOpen, nextIsClosingSoon);
      }

      nextScheduledAt = candidate.toInstant();

      var shortLabel =
          channelLabelResolver.shortLabel(first.getChannelName(), java.util.Locale.getDefault());
      nextDrawLabel = shortLabel != null ? shortLabel : channelLabel;

      nextIsOpen = now.isBefore(candidate);
      long seconds = Duration.between(now, candidate).getSeconds();
      nextIsClosingSoon = seconds >= 0 && seconds <= 300;
    }
    // ----------------------------------------------------------------------

    return buildResponse(
        first, results, nextScheduledAt, nextDrawLabel, nextIsOpen, nextIsClosingSoon);
  }

  private PublicLatestDrawResultsResponse buildResponse(
      PublicDrawResultRow first,
      List<PublicDrawResultItemResponse> results,
      Instant nextScheduledAt,
      String nextDrawLabel,
      Boolean nextIsOpen,
      Boolean nextIsClosingSoon) {

    return new PublicLatestDrawResultsResponse(
        first.getChannelCode(),
        first.getChannelName(),
        first.getChannelTimezone() == null ? null : first.getChannelTimezone().toString(),
        first.getChannelDrawTime() == null
            ? null
            : first.getChannelDrawTime().format(TIME_FORMATTER),
        results,
        nextScheduledAt,
        nextDrawLabel,
        nextIsOpen,
        nextIsClosingSoon);
  }

  private List<Integer> mapNumbers(String jsonb) {
    if (jsonb == null || jsonb.isEmpty()) return Collections.emptyList();
    try {
      return jsonUtils.readValue(jsonb, new TypeReference<>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private PublicDrawResultItemResponse mapToItemResponse(PublicDrawResultRow row) {
    String channelLabel =
        channelLabelResolver.resolve(
            row.getChannelName(), row.getChannelDrawTime(), java.util.Locale.getDefault());

    return new PublicDrawResultItemResponse(
        row.getChannelCode(),
        channelLabel,
        row.getDrawDate(),
        row.getOccurredAt(),
        mapNumbers(row.getNumbersMainJson()),
        mapNumbers(row.getNumbersExtraJson()),
        row.getQuality(),
        row.getSource());
  }
}
