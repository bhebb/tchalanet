package com.tchalanet.server.core.pagemodel.infra.event;

import com.tchalanet.server.core.pagemodel.application.port.out.PageModelWritePort;
import com.tchalanet.server.core.pagemodel.domain.event.PageModelTemplateUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

/**
 * Listener de l'événement PageModelTemplateUpdatedEvent.
 *
 * [Phase 4C] créé pour propager les changements de template vers les instances DRAFT.
 *
 * ⚠️ GAP 4C-2 : l'événement n'est pas encore publié — PageModelTemplateAdminService
 * est un @Service direct, pas un CommandHandler. Ce listener est fonctionnel dès que
 * la publication de l'event est ajoutée (via AfterCommit + ApplicationEventPublisher)
 * dans PageModelTemplateAdminService.updateFromView().
 *
 * Convention event_model.md §5.1 : @TransactionalEventListener(phase = AFTER_COMMIT)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PageModelTemplateUpdatedListener {

  private final PageModelWritePort writePort;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void on(PageModelTemplateUpdatedEvent event) {
    log.info("PageModelTemplateUpdated — propagating to instances templeId={}",
        event.templateId());
    writePort.applyTemplateUpdate(
        event.templateId(),
        event.newModel(),
        event.newSchemaVersion(),
        event.actorId()
    );
  }
}

