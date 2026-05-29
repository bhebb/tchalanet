package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketVerificationProjection;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketVerificationReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TicketVerificationJdbcAdapter implements TicketVerificationReaderPort {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String HEADER_SQL = """
        SELECT t.id                AS ticket_id,
               t.tenant_id,
               t.public_code,
               t.sale_status,
               t.result_status,
               t.settlement_status,
               t.placed_at,
               t.total_amount,
               t.winning_amount,
               t.currency,
               d.draw_date,
               d.scheduled_at,
               dc.name            AS draw_channel_name,
               o.name             AS outlet_name
        FROM sales_ticket t
        JOIN draw d ON d.id = t.draw_id
        LEFT JOIN draw_channel dc ON dc.id = t.draw_channel_id
        LEFT JOIN outlet o ON o.id = t.outlet_id
        WHERE t.public_code = :publicCode
          AND t.verification_code = :verificationCode
          AND t.deleted_at IS NULL
          AND t.sale_status NOT IN ('PENDING_APPROVAL', 'REJECTED')
        """;

    private static final String LINES_SQL = """
        SELECT line_number, game_code, bet_type, bet_option, display_selection,
               stake_amount, potential_payout_amount, origin, pricing_source,
               promotion_label
        FROM sales_ticket_line
        WHERE ticket_id = :ticketId
          AND deleted_at IS NULL
        ORDER BY line_number
        """;

    @Override
    public Optional<TicketVerificationProjection> findByPublicCodeAndVerificationCode(
        String publicCode,
        String verificationCode
    ) {
        var params = new MapSqlParameterSource()
            .addValue("publicCode", publicCode)
            .addValue("verificationCode", verificationCode);

        var rows = jdbc.query(HEADER_SQL, params, (rs, i) -> mapHeader(rs));
        if (rows.isEmpty()) return Optional.empty();

        var h = rows.get(0);
        var currency = CurrencyCode.of(h.currency());

        var lineParams = new MapSqlParameterSource("ticketId", h.ticketId());
        var lines = jdbc.query(LINES_SQL, lineParams, (rs, i) -> mapLine(rs, currency));

        return Optional.of(new TicketVerificationProjection(
            TenantId.of(h.tenantId()),
            h.publicCode(),
            displayPublicCode(h.publicCode()),
            h.saleStatus(),
            h.resultStatus(),
            h.settlementStatus(),
            h.placedAt(),
            new Money(h.totalAmount(), currency),
            h.winningAmount() != null ? new Money(h.winningAmount(), currency) : null,
            new TicketVerificationProjection.DrawProjection(
                h.drawChannelName(),
                h.drawChannelName(),
                h.drawDate(),
                h.scheduledAt()
            ),
            h.outletName() != null ? new TicketVerificationProjection.OutletProjection(h.outletName()) : null,
            lines
        ));
    }

    private HeaderRow mapHeader(ResultSet rs) throws SQLException {
        UUID ticketId = rs.getObject("ticket_id", UUID.class);
        UUID tenantId = rs.getObject("tenant_id", UUID.class);
        String publicCode = rs.getString("public_code");
        var saleStatus = parseEnum(TicketSaleStatus.class, rs.getString("sale_status"));
        var resultStatus = parseEnum(TicketResultStatus.class, rs.getString("result_status"));
        var settlementStatus = parseEnum(TicketSettlementStatus.class, rs.getString("settlement_status"));
        var placedAtTs = rs.getTimestamp("placed_at");
        Instant placedAt = placedAtTs != null ? placedAtTs.toInstant() : null;
        BigDecimal totalAmount = rs.getBigDecimal("total_amount");
        BigDecimal winningAmount = rs.getBigDecimal("winning_amount");
        String currency = rs.getString("currency");
        var drawDateVal = rs.getObject("draw_date", java.sql.Date.class);
        LocalDate drawDate = drawDateVal != null ? drawDateVal.toLocalDate() : null;
        var scheduledAtTs = rs.getTimestamp("scheduled_at");
        Instant scheduledAt = scheduledAtTs != null ? scheduledAtTs.toInstant() : null;
        String drawChannelName = rs.getString("draw_channel_name");
        String outletName = rs.getString("outlet_name");
        return new HeaderRow(ticketId, tenantId, publicCode, saleStatus, resultStatus, settlementStatus,
            placedAt, totalAmount, winningAmount, currency, drawDate, scheduledAt, drawChannelName, outletName);
    }

    private TicketVerificationProjection.LineProjection mapLine(ResultSet rs, CurrencyCode currency)
        throws SQLException {
        int lineNumber = rs.getInt("line_number");
        var gameCode = parseEnum(GameCode.class, rs.getString("game_code"));
        var betType = parseEnum(BetType.class, rs.getString("bet_type"));
        Short betOption = getNullableShort(rs, "bet_option");
        String displaySelection = rs.getString("display_selection");
        BigDecimal stakeAmount = rs.getBigDecimal("stake_amount");
        BigDecimal potentialPayout = rs.getBigDecimal("potential_payout_amount");
        var origin = rs.getString("origin");
        var pricingSource = rs.getString("pricing_source");
        var promotionLabel = rs.getString("promotion_label");
        var promotional = "PROMOTION".equals(origin)
            || "PROMOTION".equals(pricingSource)
            || (promotionLabel != null && !promotionLabel.isBlank());
        return new TicketVerificationProjection.LineProjection(
            lineNumber,
            gameCode,
            betType,
            betOption,
            gameCode == null ? null : gameCode.name(),
            betType == null ? null : betType.name(),
            optionLabel(betType, betOption),
            displaySelection,
            new Money(stakeAmount, currency),
            new Money(potentialPayout, currency),
            promotional,
            promotionLabel
        );
    }

    private Short getNullableShort(ResultSet rs, String column) throws SQLException {
        short value = rs.getShort(column);
        return rs.wasNull() ? null : value;
    }

    private String optionLabel(BetType betType, Short betOption) {
        if (betType == null || betOption == null) {
            return null;
        }
        try {
            var option = com.tchalanet.server.catalog.game.api.model.BetOption.from(betType, betOption);
            return option == null ? null : option.label();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String displayPublicCode(String publicCode) {
        if (publicCode == null || publicCode.length() != 8) {
            return publicCode;
        }
        return publicCode.substring(0, 4) + "-" + publicCode.substring(4);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Enum.valueOf(cls, val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record HeaderRow(
        UUID ticketId, UUID tenantId, String publicCode,
        TicketSaleStatus saleStatus, TicketResultStatus resultStatus,
        TicketSettlementStatus settlementStatus,
        Instant placedAt, BigDecimal totalAmount, BigDecimal winningAmount,
        String currency, LocalDate drawDate, Instant scheduledAt,
        String drawChannelName, String outletName
    ) {}
}
