package com.tchalanet.server.platform.notification.internal.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.platform.notification.internal.rule.PayoutNotificationRule;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

class NotificationDomainEventRouterTest {

  @Test
  void duplicateEventSkipsNotificationCreation() {
    var commandBus = mock(CommandBus.class);
    var processedEvents = provider(false);
    var router =
        new NotificationDomainEventRouter(
            List.of(new PayoutNotificationRule()),
            commandBus,
            new JsonUtils(JsonMapper.builder().build()),
            processedEvents);

    router.on(new PayoutRequestedEvent(UUID.randomUUID(), TenantId.of(UUID.randomUUID())));

    verify(commandBus, never()).execute(any(Command.class));
  }

  @Test
  void newEventCreatesNotification() {
    var commandBus = mock(CommandBus.class);
    var processedEvents = provider(true);
    var router =
        new NotificationDomainEventRouter(
            List.of(new PayoutNotificationRule()),
            commandBus,
            new JsonUtils(JsonMapper.builder().build()),
            processedEvents);

    router.on(new PayoutRequestedEvent(UUID.randomUUID(), TenantId.of(UUID.randomUUID())));

    verify(commandBus).execute(any(Command.class));
  }

  private static ObjectProvider<ProcessedEventPort> provider(boolean markProcessedResult) {
    var port = mock(ProcessedEventPort.class);
    when(port.markProcessedIfAbsent(any(), any())).thenReturn(markProcessedResult);

    @SuppressWarnings("unchecked")
    ObjectProvider<ProcessedEventPort> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(port);
    return provider;
  }

  private record PayoutRequestedEvent(UUID eventId, TenantId tenantId) {}
}
