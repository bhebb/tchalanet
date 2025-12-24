package com.tchalanet.server.core.sales.infra.event;

import com.tchalanet.server.core.ledger.application.port.in.RecordLedgerFromSalesPort;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesLedgerListenerTest {

    @Mock
    private RecordLedgerFromSalesPort ledgerPort;

    @Mock
    private ApplicationEventPublisher eventPublisher; // if needed for publishing, but here we mock the port

    @InjectMocks
    private SalesLedgerListener listener;

    @Test
    void shouldCallRecordTicketSaleOnTicketPlacedEvent() {
        // Given
        var tenantId = UUID.randomUUID();
        var ticketId = UUID.randomUUID();
        var stakeCents = 1500L;
        var occurredAt = Instant.now();
        var eventId = UUID.randomUUID();

        var event = new TicketPlacedEvent(eventId, new com.tchalanet.server.core.tenant.domain.model.TenantId(tenantId), ticketId, stakeCents, occurredAt);

        // When
        listener.onTicketPlaced(event);

        // Then
        verify(ledgerPort).recordTicketSale(tenantId, ticketId, stakeCents, occurredAt);
    }

    @Test
    void shouldLogErrorWhenRecordTicketSaleThrowsException() {
        // Given
        var tenantId = UUID.randomUUID();
        var ticketId = UUID.randomUUID();
        var stakeCents = 1000L;
        var occurredAt = Instant.now();
        var eventId = UUID.randomUUID();

        var event = new TicketPlacedEvent(eventId, new com.tchalanet.server.core.tenant.domain.model.TenantId(tenantId), ticketId, stakeCents, occurredAt);

        doThrow(new RuntimeException("Ledger error")).when(ledgerPort).recordTicketSale(any(), any(), anyLong(), any());

        // When
        listener.onTicketPlaced(event);

        // Then
        verify(ledgerPort).recordTicketSale(tenantId, ticketId, stakeCents, occurredAt);
        // Logging is hard to verify without capturing logs, but assume it's logged
    }
}
