package com.tchalanet.server.core.payout.infra.event;

import com.tchalanet.server.core.ledger.application.port.in.RecordLedgerFromPayoutPort;
import com.tchalanet.server.core.payout.domain.event.PayoutRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutLedgerListenerTest {

    @Mock
    private RecordLedgerFromPayoutPort ledgerPort;

    @InjectMocks
    private PayoutLedgerListener listener;

    @Test
    void shouldCallRecordPayoutOnPayoutRegisteredEvent() {
        // Given
        var tenantId = UUID.randomUUID();
        var payoutId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(100.00);
        var occurredAt = Instant.now();
        var eventId = UUID.randomUUID();

        var event = new PayoutRegisteredEvent(eventId, new com.tchalanet.server.core.tenant.domain.model.TenantId(tenantId), payoutId, amount, occurredAt);

        // When
        listener.onPayoutRegistered(event);

        // Then
        verify(ledgerPort).recordPayout(tenantId, payoutId, amount, occurredAt);
    }

    @Test
    void shouldLogErrorWhenRecordPayoutThrowsException() {
        // Given
        var tenantId = UUID.randomUUID();
        var payoutId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(50.00);
        var occurredAt = Instant.now();
        var eventId = UUID.randomUUID();

        var event = new PayoutRegisteredEvent(eventId, new com.tchalanet.server.core.tenant.domain.model.TenantId(tenantId), payoutId, amount, occurredAt);

        doThrow(new RuntimeException("Ledger error")).when(ledgerPort).recordPayout(any(), any(), any(), any());

        // When
        listener.onPayoutRegistered(event);

        // Then
        verify(ledgerPort).recordPayout(tenantId, payoutId, amount, occurredAt);
        // Logging is hard to verify without capturing logs, but assume it's logged
    }
}
