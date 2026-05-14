package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.TicketSettlementStatus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.query.GetPublicTicketVerificationRecordQuery;
import com.tchalanet.server.core.sales.api.query.PublicTicketVerificationLineRecord;
import com.tchalanet.server.core.sales.api.query.PublicTicketVerificationRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetPublicTicketVerificationRecordQueryHandler
    implements QueryHandler<GetPublicTicketVerificationRecordQuery, PublicTicketVerificationRecord> {

  private final NamedParameterJdbcTemplate jdbc;

  private static final String HEADER_SQL = """
      SELECT ticket_id, tenant_id, public_code,
             sale_status, result_status, settlement_status,
             sold_at, total_amount, winning_amount,
             outlet_name, outlet_city, outlet_country
      FROM v_ticket_print
      WHERE public_code = :publicCode
      """;

  private static final String LINES_SQL = """
      SELECT game_code, bet_type, selection, stake, potential_payout
      FROM ticket_line
      WHERE ticket_id = :ticketId
        AND deleted_at IS NULL
      ORDER BY created_at
      """;

  @Override
  public PublicTicketVerificationRecord handle(GetPublicTicketVerificationRecordQuery query) {
    var params = new MapSqlParameterSource("publicCode", query.publicCode());
    var rows = jdbc.query(HEADER_SQL, params, (rs, i) -> mapHeader(rs));
    if (rows.isEmpty()) return null;

    var h = rows.get(0);
    var lineParams = new MapSqlParameterSource("ticketId", h.ticketId());
    var lines = jdbc.query(LINES_SQL, lineParams, (rs, i) -> mapLine(rs));

    return new PublicTicketVerificationRecord(
        TenantId.of(h.tenantId()),
        h.publicCode(),
        h.saleStatus(),
        h.resultStatus(),
        h.settlementStatus(),
        h.soldAt(),
        h.totalAmount(),
        h.winningAmount(),
        h.outletName(),
        h.outletCity(),
        h.outletCountry(),
        lines
    );
  }

  private HeaderRow mapHeader(ResultSet rs) throws SQLException {
    UUID ticketId = rs.getObject("ticket_id", UUID.class);
    UUID tenantId = rs.getObject("tenant_id", UUID.class);
    String publicCode = rs.getString("public_code");
    TicketSaleStatus saleStatus = parseEnum(TicketSaleStatus.class, rs.getString("sale_status"));
    TicketResultStatus resultStatus = parseEnum(TicketResultStatus.class, rs.getString("result_status"));
    TicketSettlementStatus settlementStatus = parseEnum(TicketSettlementStatus.class, rs.getString("settlement_status"));
    var soldAtTs = rs.getTimestamp("sold_at");
    Instant soldAt = soldAtTs != null ? soldAtTs.toInstant() : null;
    BigDecimal totalAmount = rs.getBigDecimal("total_amount");
    BigDecimal winningAmount = rs.getBigDecimal("winning_amount");
    String outletName = rs.getString("outlet_name");
    String outletCity = rs.getString("outlet_city");
    String outletCountry = rs.getString("outlet_country");
    return new HeaderRow(ticketId, tenantId, publicCode, saleStatus, resultStatus, settlementStatus,
        soldAt, totalAmount, winningAmount, outletName, outletCity, outletCountry);
  }

  private PublicTicketVerificationLineRecord mapLine(ResultSet rs) throws SQLException {
    String gameCode = rs.getString("game_code");
    BetType betType = parseEnum(BetType.class, rs.getString("bet_type"));
    String selection = rs.getString("selection");
    BigDecimal stake = rs.getBigDecimal("stake");
    BigDecimal payout = rs.getBigDecimal("potential_payout");
    return new PublicTicketVerificationLineRecord(gameCode, betType, selection, stake, payout);
  }

  private <E extends Enum<E>> E parseEnum(Class<E> cls, String val) {
    if (val == null || val.isBlank()) return null;
    try { return Enum.valueOf(cls, val); } catch (IllegalArgumentException e) { return null; }
  }

  private record HeaderRow(
      UUID ticketId, UUID tenantId, String publicCode,
      TicketSaleStatus saleStatus, TicketResultStatus resultStatus,
      TicketSettlementStatus settlementStatus,
      Instant soldAt, BigDecimal totalAmount, BigDecimal winningAmount,
      String outletName, String outletCity, String outletCountry
  ) {}
}
