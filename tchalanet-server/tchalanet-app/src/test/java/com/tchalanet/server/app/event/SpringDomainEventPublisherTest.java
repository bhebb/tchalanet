package com.tchalanet.server.app.event;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("SpringDomainEventPublisher")
class SpringDomainEventPublisherTest {

    record TestEvent(EventId eventId, Instant occurredAt, TenantId tenantId) implements DomainEvent {}

    private static TestEvent event(String suffix) {
        return new TestEvent(
            EventId.of(UUID.randomUUID()),
            Instant.now(),
            TenantId.of(UUID.fromString("11111111-1111-1111-1111-11111111111" + suffix)));
    }

    @Nested
    @DisplayName("publish(single)")
    class PublishSingle {

        @Test
        @DisplayName("should delegate exactly once to ApplicationEventPublisher")
        void shouldDelegateOnce() {
            var delegate = mock(ApplicationEventPublisher.class);
            var publisher = new SpringDomainEventPublisher(delegate);
            var evt = event("1");

            publisher.publish(evt);

            verify(delegate).publishEvent(evt);
        }
    }

    @Nested
    @DisplayName("publish(collection)")
    class PublishCollection {

        @Test
        @DisplayName("should delegate each event in order")
        void shouldDelegateEachInOrder() {
            var delegate = mock(ApplicationEventPublisher.class);
            var publisher = new SpringDomainEventPublisher(delegate);
            var e1 = event("1");
            var e2 = event("2");
            var e3 = event("3");

            publisher.publish(List.of(e1, e2, e3));

            var order = inOrder(delegate);
            order.verify(delegate).publishEvent(e1);
            order.verify(delegate).publishEvent(e2);
            order.verify(delegate).publishEvent(e3);
        }

        @Test
        @DisplayName("should do nothing for empty collection")
        void shouldHandleEmptyCollection() {
            var delegate = mock(ApplicationEventPublisher.class);
            var publisher = new SpringDomainEventPublisher(delegate);

            publisher.publish(List.of());

            org.mockito.Mockito.verifyNoInteractions(delegate);
        }
    }
}
