package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.application.formatter.DrawLabelFormat;
import com.tchalanet.server.core.sales.application.formatter.TicketDrawLabelFormatter;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintLine;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Primary
@Component
@RequiredArgsConstructor
public class JdbcTicketPrintReaderAdapter implements TicketPrintReaderPort {

  private final NamedParameterJdbcTemplate jdbc;
  private final TicketDrawLabelFormatter labelFormatter;

  private static final String HEADER_SQL = """
      SELECT ticket_id, ticket_code, public_code, terminal_id, session_id,
             draw_id, sold_at, total_amount, outlet_name,
             draw_channel_code, draw_channel_label, draw_time, draw_timezone,
             draw_date, scheduled_at
      FROM v_ticket_print
      WHERE ticket_id = :ticketId
      """;

  private static final String LINES_SQL = """
      SELECT game_code, bet_type, bet_option, selection, stake, potential_payout
      FROM ticket_line
      WHERE ticket_id = :ticketId
        AND deleted_at IS NULL
      ORDER BY created_at
      """;

  @Override
  public Optional<TicketPrintView> findTicketPrintView(TicketId ticketId, Locale locale) {
    var params = new MapSqlParameterSource("ticketId", ticketId.value());

    var rows = jdbc.query(HEADER_SQL, params, (rs, i) -> mapHeader(rs, locale != null ? locale : Locale.FRENCH));
    if (rows.isEmpty()) {
      return Optional.empty();
    }

    var header = rows.get(0);
    var lines = jdbc.query(LINES_SQL, params, (rs, i) -> mapLine(rs));

    return Optional.of(new TicketPrintView(
        header.ticketId(),
        header.ticketCode(),
        header.publicCode(),
        header.terminalId(),
        header.drawId(),
        header.soldAt(),
        header.totalAmount(),
        header.outletName(),
        header.channelCode(),
        header.drawChannelLabel(),
        header.drawWhenLabel(),
        lines
    ));
  }

  private HeaderRow mapHeader(ResultSet rs, Locale locale) throws SQLException {
    UUID ticketId   = rs.getObject("ticket_id", UUID.class);
    String code     = rs.getString("ticket_code");
    String pubCode  = rs.getString("public_code");
    UUID terminalId = rs.getObject("terminal_id", UUID.class);
    UUID drawId     = rs.getObject("draw_id", UUID.class);
    Instant soldAt  = toInstant(rs, "sold_at");
    BigDecimal amt  = rs.getBigDecimal("total_amount");
    String outlet   = rs.getString("outlet_name");
    String chCode   = rs.getString("draw_channel_code");
    String chLabel  = rs.getString("draw_channel_label");

    LocalDate drawDate  = rs.getObject("draw_date", LocalDate.class);
    LocalTime drawTime  = rs.getObject("draw_time", LocalTime.class);
    String tz           = rs.getString("draw_timezone");
    Instant scheduledAt = toInstant(rs, "scheduled_at");

    ZoneId zone = tz != null ? ZoneId.of(tz) : ZoneId.of("UTC");
    String whenLabel = labelFormatter.format(drawDate, drawTime, zone, scheduledAt, locale, DrawLabelFormat.TICKET_SHORT);

    return new HeaderRow(ticketId, code, pubCode, terminalId, drawId, soldAt, amt, outlet, chCode, chLabel, whenLabel);
  }

  private TicketPrintLine mapLine(ResultSet rs) throws SQLException {
    String gameCode = rs.getString("game_code");
    String betTypeStr = rs.getString("bet_type");
    BetType betType = betTypeStr != null ? BetType.valueOf(betTypeStr) : null;
    short betOption = rs.getShort("bet_option");
    Short betOptionVal = rs.wasNull() ? null : betOption;
    String selection = rs.getString("selection");
    BigDecimal stake = rs.getBigDecimal("stake");
    BigDecimal payout = rs.getBigDecimal("potential_payout");
    return new TicketPrintLine(gameCode, betType, betOptionVal, selection, stake, payout);
  }

  private Instant toInstant(ResultSet rs, String col) throws SQLException {
    var ts = rs.getTimestamp(col);
    return ts != null ? ts.toInstant() : null;
  }

  private record HeaderRow(
      UUID ticketId,
      String ticketCode,
      String publicCode,
      UUID terminalId,
      UUID drawId,
      Instant soldAt,
      BigDecimal totalAmount,
      String outletName,
      String channelCode,
      String drawChannelLabel,
      String drawWhenLabel
  ) {}
}
