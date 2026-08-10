package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

class ExposureProjectorAdapterTest {

  @Test
  void usesEventCanonicalSelectionWhenBetOptionIsImplicit() {
    var jdbc = new CapturingJdbcTemplate();
    var adapter = new ExposureProjectorAdapter(jdbc);

    adapter.applyTicketSold(ticketPlacedWithImplicitMaryajLine());

    assertThat(jdbc.calls).hasSize(2);
    assertThat(jdbc.calls)
        .allSatisfy(
            args -> {
              assertThat(args[4]).isEqualTo(BetType.MARRIAGE_2D2D.name());
              assertThat(args[5]).isEqualTo("12-34");
              assertThat((BigDecimal) args[6]).isEqualByComparingTo("10.00");
            });
  }

  private static TicketPlacedEvent ticketPlacedWithImplicitMaryajLine() {
    var tenantId = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    var drawId = DrawId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    var drawChannelId = DrawChannelId.of(UUID.fromString("30000000-0000-0000-0000-000000000001"));
    var currency = CurrencyCode.of("HTG");

    return new TicketPlacedEvent(
        EventId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
        TicketPlacedEvent.CURRENT_SCHEMA,
        Instant.parse("2026-08-09T11:47:54Z"),
        CorrelationId.of(UUID.fromString("50000000-0000-0000-0000-000000000001")),
        tenantId,
        TicketId.of(UUID.fromString("60000000-0000-0000-0000-000000000001")),
        TicketSaleStatus.APPROVED,
        TicketSaleChannel.POS_ONLINE,
        new TicketContextPayload(drawId, drawChannelId, null, null, null),
        new TicketMoneyPayload(
            currency, money("10.00", currency), money("10.00", currency), List.of()),
        List.of(
            new TicketLinePlacedItem(
                TicketLineId.of(UUID.fromString("70000000-0000-0000-0000-000000000001")),
                1,
                GameCode.HT_MARYAJ,
                BetType.MARRIAGE_2D2D,
                "12-34",
                "12-34",
                null,
                money("10.00", currency),
                TicketLineOrigin.CUSTOMER,
                TicketLinePricingSource.STANDARD,
                TicketLineSelectionSource.CUSTOMER_SELECTED,
                null,
                null,
                null)),
        null);
  }

  private static Money money(String amount, CurrencyCode currency) {
    return new Money(new BigDecimal(amount), currency);
  }

  private static final class CapturingJdbcTemplate extends JdbcTemplate {
    private final List<Object[]> calls = new java.util.ArrayList<>();

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> rse, Object... args)
        throws DataAccessException {
      calls.add(args);
      try {
        return rse.extractData(null);
      } catch (SQLException ex) {
        throw new DataAccessException("capture failed", ex) {};
      }
    }
  }
}
