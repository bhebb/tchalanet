package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordTicketSaleLedgerCommandHandlerTest {

    @Mock
    private LedgerWriterPort ledgerWriter;

    @Mock
    private LedgerReaderPort ledgerReader;

    @Mock
    private Clock clock;

    @InjectMocks
    private RecordTicketSaleLedgerCommandHandler handler;

    @Test
    void shouldRecordTicketSaleWhenNoExistingEntry() {
        // Given
        var tenantId = UUID.randomUUID();
        var ticketId = UUID.randomUUID();
        var stakeCents = 1000L;
        var occurredAt = Instant.now();
        var fixedInstant = Instant.parse("2023-01-01T10:00:00Z");

        when(ledgerReader.findByRef(tenantId, LedgerRefType.TICKET_SALE, ticketId)).thenReturn(Collections.emptyList());
        when(clock.instant()).thenReturn(fixedInstant);

        // When
        handler.recordTicketSale(tenantId, ticketId, stakeCents, occurredAt);

        // Then
        verify(ledgerWriter).append(any(LedgerEntry.class));
        verify(ledgerReader).findByRef(tenantId, LedgerRefType.TICKET_SALE, ticketId);
    }

    @Test
    void shouldSkipRecordingWhenEntryAlreadyExists() {
        // Given
        var tenantId = UUID.randomUUID();
        var ticketId = UUID.randomUUID();
        var stakeCents = 1000L;
        var occurredAt = Instant.now();

        var existingEntry = LedgerEntry.create(tenantId, LedgerRefType.TICKET_SALE, ticketId, BigDecimal.TEN, LedgerDirection.CREDIT, Instant.now());
        when(ledgerReader.findByRef(tenantId, LedgerRefType.TICKET_SALE, ticketId)).thenReturn(Collections.singletonList(existingEntry));

        // When
        handler.recordTicketSale(tenantId, ticketId, stakeCents, occurredAt);

        // Then
        verify(ledgerWriter, never()).append(any(LedgerEntry.class));
        verify(ledgerReader).findByRef(tenantId, LedgerRefType.TICKET_SALE, ticketId);
    }

    @Test
    void shouldUseProvidedOccurredAt() {
        // Given
        var tenantId = UUID.randomUUID();
        var ticketId = UUID.randomUUID();
        var stakeCents = 500L;
        var occurredAt = Instant.parse("2023-01-01T10:00:00Z");

        when(ledgerReader.findByRef(tenantId, LedgerRefType.TICKET_SALE, ticketId)).thenReturn(Collections.emptyList());

        // When
        handler.recordTicketSale(tenantId, ticketId, stakeCents, occurredAt);

        // Then
        verify(ledgerWriter).append(argThat(entry ->
            entry.occurredAt().equals(occurredAt) &&
            entry.amount().equals(BigDecimal.valueOf(5.00)) &&
            entry.direction() == LedgerDirection.CREDIT &&
            entry.refType() == LedgerRefType.TICKET_SALE
        ));
    }

    @Test
    void shouldUseClockWhenOccurredAtIsNull() {
        // Given
        var tenantId = UUID.randomUUID();
        var ticketId = UUID.randomUUID();
        var stakeCents = 2000L;
        var fixedInstant = Instant.parse("2023-01-01T10:00:00Z");

        when(ledgerReader.findByRef(tenantId, LedgerRefType.TICKET_SALE, ticketId)).thenReturn(Collections.emptyList());
        when(clock.instant()).thenReturn(fixedInstant);

        // When
        handler.recordTicketSale(tenantId, ticketId, stakeCents, null);

        // Then
        verify(ledgerWriter).append(argThat(entry ->
            entry.occurredAt().equals(fixedInstant) &&
            entry.amount().equals(BigDecimal.valueOf(20.00))
        ));
    }
}
