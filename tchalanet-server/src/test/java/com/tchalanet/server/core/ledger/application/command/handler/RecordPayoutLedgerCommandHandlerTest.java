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
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordPayoutLedgerCommandHandlerTest {

    @Mock
    private LedgerWriterPort ledgerWriter;

    @Mock
    private LedgerReaderPort ledgerReader;

    @Mock
    private Clock clock;

    @InjectMocks
    private RecordPayoutLedgerCommandHandler handler;

    @Test
    void shouldRecordPayoutWhenNoExistingEntry() {
        // Given
        var tenantId = UUID.randomUUID();
        var payoutId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(100.00);
        var occurredAt = Instant.now();
        var fixedInstant = Instant.parse("2023-01-01T10:00:00Z");

        when(ledgerReader.findByRef(tenantId, LedgerRefType.PAYOUT, payoutId)).thenReturn(Collections.emptyList());
        when(clock.instant()).thenReturn(fixedInstant);

        // When
        handler.recordPayout(tenantId, payoutId, amount, occurredAt);

        // Then
        verify(ledgerWriter).append(any(LedgerEntry.class));
        verify(ledgerReader).findByRef(tenantId, LedgerRefType.PAYOUT, payoutId);
    }

    @Test
    void shouldSkipRecordingWhenEntryAlreadyExists() {
        // Given
        var tenantId = UUID.randomUUID();
        var payoutId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(50.00);
        var occurredAt = Instant.now();

        var existingEntry = LedgerEntry.create(tenantId, LedgerRefType.PAYOUT, payoutId, BigDecimal.TEN, LedgerDirection.DEBIT, Instant.now());
        when(ledgerReader.findByRef(tenantId, LedgerRefType.PAYOUT, payoutId)).thenReturn(Collections.singletonList(existingEntry));

        // When
        handler.recordPayout(tenantId, payoutId, amount, occurredAt);

        // Then
        verify(ledgerWriter, never()).append(any(LedgerEntry.class));
        verify(ledgerReader).findByRef(tenantId, LedgerRefType.PAYOUT, payoutId);
    }

    @Test
    void shouldUseProvidedOccurredAt() {
        // Given
        var tenantId = UUID.randomUUID();
        var payoutId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(25.00);
        var occurredAt = Instant.parse("2023-01-01T10:00:00Z");

        when(ledgerReader.findByRef(tenantId, LedgerRefType.PAYOUT, payoutId)).thenReturn(Collections.emptyList());

        // When
        handler.recordPayout(tenantId, payoutId, amount, occurredAt);

        // Then
        verify(ledgerWriter).append(argThat(entry ->
            entry.occurredAt().equals(occurredAt) &&
            entry.amount().equals(BigDecimal.valueOf(25.00)) &&
            entry.direction() == LedgerDirection.DEBIT &&
            entry.refType() == LedgerRefType.PAYOUT
        ));
    }

    @Test
    void shouldUseClockWhenOccurredAtIsNull() {
        // Given
        var tenantId = UUID.randomUUID();
        var payoutId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(75.00);
        var fixedInstant = Instant.parse("2023-01-01T10:00:00Z");

        when(ledgerReader.findByRef(tenantId, LedgerRefType.PAYOUT, payoutId)).thenReturn(Collections.emptyList());
        when(clock.instant()).thenReturn(fixedInstant);

        // When
        handler.recordPayout(tenantId, payoutId, amount, null);

        // Then
        verify(ledgerWriter).append(argThat(entry ->
            entry.occurredAt().equals(fixedInstant) &&
            entry.amount().equals(BigDecimal.valueOf(75.00))
        ));
    }
}
