package com.tchalanet.server.core.limitpolicy.internal.application.command.handler.exposure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.tchalanet.server.core.limitpolicy.api.command.ApplyTicketExposureCommand;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure.ExposureProjectorPort;
import com.tchalanet.server.core.sales.api.event.TicketLinePlacedItem;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplyTicketExposureCommandHandlerTest {

  private static final String HANDLER_KEY = "limitpolicy.exposure";

  private final ProcessedEventPort processedEvent = mock(ProcessedEventPort.class);
  private final ExposureProjectorPort projector = mock(ExposureProjectorPort.class);

  private final ApplyTicketExposureCommandHandler handler =
      new ApplyTicketExposureCommandHandler(processedEvent, projector);

  @Test
  void processes_event_and_delegates_to_projector_first_time() {
    var event = ticketEvent();
    when(processedEvent.markProcessedIfAbsent(HANDLER_KEY, event.eventId().value()))
        .thenReturn(true);

    handler.handle(new ApplyTicketExposureCommand(event));

    verify(projector).applyTicketSold(event);
  }

  @Test
  void skips_projector_when_event_already_processed() {
    var event = ticketEvent();
    when(processedEvent.markProcessedIfAbsent(HANDLER_KEY, event.eventId().value()))
        .thenReturn(false);

    handler.handle(new ApplyTicketExposureCommand(event));

    verify(projector, never()).applyTicketSold(event);
  }

  private static TicketPlacedEvent ticketEvent() {
    var currency = CurrencyCode.of("HTG");
    return new TicketPlacedEvent(
        EventId.of(UUID.fromString("40000000-0000-0000-0000-000000000099")),
        TicketPlacedEvent.CURRENT_SCHEMA,
        Instant.parse("2026-08-12T10:00:00Z"),
        CorrelationId.of(UUID.randomUUID()),
        TenantId.of(UUID.randomUUID()),
        TicketId.of(UUID.randomUUID()),
        TicketSaleStatus.APPROVED,
        TicketSaleChannel.POS_ONLINE,
        new TicketContextPayload(
            DrawId.of(UUID.randomUUID()), DrawChannelId.of(UUID.randomUUID()), null, null, null),
        new TicketMoneyPayload(
            currency,
            new Money(new BigDecimal("100.00"), currency),
            new Money(new BigDecimal("100.00"), currency),
            List.of()),
        List.of(
            new TicketLinePlacedItem(
                TicketLineId.of(UUID.randomUUID()),
                1,
                GameCode.HT_BOLET,
                BetType.MATCH_1_2D,
                "12",
                "12",
                null,
                new Money(new BigDecimal("100.00"), currency),
                TicketLineOrigin.CUSTOMER,
                TicketLinePricingSource.STANDARD,
                TicketLineSelectionSource.CUSTOMER_SELECTED,
                null,
                null,
                null)),
        null);
  }
}
