package com.tchalanet.server.features.pagemodel.template;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.tchalanet.server.catalog.pagemodeltemplate.api.event.PageModelTemplateUpdatedEvent;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelWritePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PageModelTemplateUpdatedListener {

  private final PageModelWritePort writePort;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void on(PageModelTemplateUpdatedEvent event) {
    log.info("PageModelTemplateUpdated: preparing drafts for templateId={}", event.templateId());
    writePort.applyTemplateUpdate(
        event.templateId(),
        event.logicalId(),
        event.newModel(),
        event.newSchemaVersion(),
        event.actorId());
  }
}
