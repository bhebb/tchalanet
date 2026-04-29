package com.tchalanet.server.core.pagemodel.infra.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.pagemodeltemplate.api.event.PageModelTemplateUpdatedEvent;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.core.pagemodel.application.command.model.CreatePageTemplateUpdateNotificationsCommand;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PageModelTemplateUpdatedListenerTest {

  @Test
  void shouldDispatchNotificationCommandInsteadOfMutatingPageModelsDirectly() {
    var bus = new CapturingCommandBus();
    var listener = new PageModelTemplateUpdatedListener(bus);
    var templateId = PageModelTemplateId.of(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    listener.on(new PageModelTemplateUpdatedEvent(templateId, "private.dashboard.tenant_admin", null, 4, null, Instant.now()));

    assertThat(bus.lastCommand).isInstanceOf(CreatePageTemplateUpdateNotificationsCommand.class);
    var command = (CreatePageTemplateUpdateNotificationsCommand) bus.lastCommand;
    assertThat(command.templateId()).isEqualTo(templateId);
    assertThat(command.logicalId()).isEqualTo("private.dashboard.tenant_admin");
    assertThat(command.newSchemaVersion()).isEqualTo(4);
  }

  private static final class CapturingCommandBus implements CommandBus {
    private Command<?> lastCommand;

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
      lastCommand = command;
      return null;
    }
  }
}
