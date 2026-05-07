package com.tchalanet.server.core.pagemodel.infra.event;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.tchalanet.server.catalog.pagemodeltemplate.api.event.PageModelTemplateUpdatedEvent;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.pagemodel.application.command.model.CreatePageTemplateUpdateNotificationsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PageModelTemplateUpdatedListener {

  private final CommandBus commandBus;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void on(PageModelTemplateUpdatedEvent event) {
    log.info("PageModelTemplateUpdated: notifying affected tenants for templateId={}", event.templateId());
    commandBus.execute(
        new CreatePageTemplateUpdateNotificationsCommand(
        event.templateId(),
        event.logicalId(),
        event.newModel(),
        event.newSchemaVersion(),
        event.actorId()));
  }
}
