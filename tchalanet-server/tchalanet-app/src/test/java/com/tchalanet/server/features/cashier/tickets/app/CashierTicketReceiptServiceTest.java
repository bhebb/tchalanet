package com.tchalanet.server.features.cashier.tickets.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptMessageContent;
import com.tchalanet.server.core.sales.api.query.receipt.FormatTicketReceiptMessageQuery;
import com.tchalanet.server.features.cashier.operationalcontext.ResolveSellerOperationalContextRequest;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperation;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextResolver;
import com.tchalanet.server.features.cashier.operationalcontext.SellerOperationalContextView;
import com.tchalanet.server.features.cashier.tickets.model.SendTicketReceiptRequest;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.MessageId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CashierTicketReceiptServiceTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  private static final UserId USER_ID =
      UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
  private static final TerminalId TERMINAL_ID =
      TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
  private static final OutletId OUTLET_ID =
      OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
  private static final SalesSessionId SESSION_ID =
      SalesSessionId.of(UUID.fromString("00000000-0000-0000-0000-000000000005"));
  private static final TicketId TICKET_ID =
      TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001"));

  @Test
  void sendUsesCanonicalReceiptMessageAndExplicitCommunicationContext() {
    var queryBus = new CapturingQueryBus();
    var communicationApi = new CapturingCommunicationApi();
    var sellerContextResolver = new CapturingSellerContextResolver();
    var service = new CashierTicketReceiptService(
        queryBus,
        new NoopCommandBus(),
        null,
        communicationApi,
        null,
        null,
        null,
        sellerContextResolver);

    var response = service.send(
        context(),
        TICKET_ID,
        new SendTicketReceiptRequest(
            TERMINAL_ID.value(),
            CommunicationChannel.SMS,
            "+15145550100",
            null,
            Locale.ENGLISH));

    assertEquals(SellerOperation.SEND_RECEIPT, sellerContextResolver.lastOperation);
    assertNotNull(queryBus.lastMessageQuery);
    assertEquals(TICKET_ID, queryBus.lastMessageQuery.ticketId());
    assertEquals(Locale.ENGLISH, queryBus.lastMessageQuery.locale());

    var outbound = communicationApi.lastRequest;
    assertNotNull(outbound);
    assertEquals("TICKET_RECEIPT", outbound.type());
    assertEquals(CommunicationChannel.SMS, outbound.channel());
    assertEquals(TENANT_ID, outbound.recipient().tenantId());
    assertEquals(USER_ID, outbound.recipient().userId());
    assertEquals("+15145550100", outbound.recipient().to());
    assertEquals(Locale.ENGLISH, outbound.locale());
    assertEquals("canonical subject", outbound.subject());
    assertEquals("canonical body", outbound.body());
    assertEquals("PUB123", outbound.metadata().get("publicCode"));
    assertEquals(
        TICKET_ID.value() + ":SMS:+15145550100",
        outbound.metadata().get("idempotencyKey"));
    assertEquals(true, response.queued());
  }

  private static TchRequestContext context() {
    return new TchRequestContext(
        "demo",
        TENANT_ID.value(),
        "demo",
        TENANT_ID.value(),
        "keycloak-user",
        USER_ID.value(),
        EnumSet.of(TchRole.CASHIER),
        Set.of(),
        Locale.CANADA_FRENCH,
        "req-1",
        "127.0.0.1",
        "test",
        false,
        null,
        "active",
        ApiScope.TENANT,
        null,
        TENANT_ID,
        ZoneId.of("America/Toronto"),
        Currency.getInstance("CAD"),
        new OperationalContextHint(
            TERMINAL_ID,
            OUTLET_ID,
            SESSION_ID,
            OperationalContextSource.ADMIN_SELECTION,
            OperationalContextTrust.WEAK));
  }

  private static final class CapturingQueryBus implements QueryBus {
    private FormatTicketReceiptMessageQuery lastMessageQuery;

    @Override
    @SuppressWarnings("unchecked")
    public <R> R ask(Query<R> query) {
      if (query instanceof FormatTicketReceiptMessageQuery messageQuery) {
        lastMessageQuery = messageQuery;
        return (R) new TicketReceiptMessageContent(
            "canonical subject",
            "canonical body",
            Locale.ENGLISH,
            Map.of("publicCode", "PUB123"));
      }
      throw new UnsupportedOperationException(query.getClass().getName());
    }
  }

  private static final class NoopCommandBus implements CommandBus {
    @Override
    public <R> R execute(Command<R> command) {
      return null;
    }
  }

  private static final class CapturingCommunicationApi implements CommunicationApi {
    private SendOutboundMessageRequest lastRequest;

    @Override
    public MessageId enqueue(SendOutboundMessageRequest request) {
      lastRequest = request;
      return null;
    }

    @Override
    public SendOutboundMessageResult sendNow(SendOutboundMessageRequest request) {
      return null;
    }
  }

  private static final class CapturingSellerContextResolver extends SellerOperationalContextResolver {
    private SellerOperation lastOperation;

    private CapturingSellerContextResolver() {
      super(null, null, null);
    }

    @Override
    public SellerOperationalContextView resolve(ResolveSellerOperationalContextRequest request) {
      lastOperation = request.operation();
      return new SellerOperationalContextView(
          TENANT_ID,
          USER_ID,
          TERMINAL_ID,
          OUTLET_ID,
          SESSION_ID,
          Locale.CANADA_FRENCH,
          ZoneId.of("America/Toronto"),
          Currency.getInstance("CAD"),
          Set.of("TICKET_SELL"));
    }
  }
}
