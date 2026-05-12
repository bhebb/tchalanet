package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.api.command.ResetPageModelCommand;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReaderPort;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelWriterPort;
import com.tchalanet.server.core.pagemodel.internal.domain.event.PageModelResetEvent;
import com.tchalanet.server.core.pagemodel.internal.infra.web.PageModelAdminMapper;
import com.tchalanet.server.core.pagemodel.internal.infra.web.dto.PageModelAdminDetailDto;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

/**
 * Réinitialise un PageModel à partir du modèle par défaut de son template lié.
 * Si pas de template : remet le modèle à {}.
 *
 * Après reset, publie after-commit un PageModelResetEvent
 * pour invalider le cache BFF public de l'instance.
 *
 * Conforme event_model.md §4.1 (AfterCommit) + command_query_handlers.md §3.2.
 */
@UseCase
@RequiredArgsConstructor
public class ResetPageModelCommandHandler
    implements CommandHandler<ResetPageModelCommand, PageModelAdminDetailDto> {

  private final PageModelReaderPort reader;
  private final PageModelWriterPort writer;
  private final PageModelTemplateCatalog templateCatalog;
  private final PageModelAdminMapper mapper;
  private final DomainEventPublisher events;
  private final IdGenerator idGenerator;
  private final JsonUtils jsonUtils;
  private final Clock clock;

  @Override
  @TchTx
  public PageModelAdminDetailDto handle(ResetPageModelCommand cmd) {
    var instance = reader.findById(cmd.id())
        .orElseThrow(() -> ProblemRest.notFound("pagemodel.not_found", cmd.id()));

    // Charge le modèle par défaut depuis le template lié (si templateId présent)
    // ou remet à {} si pas de template (conforme UpsertPageModelHandler — fallback vide)
    var defaultModel = instance.templateId()
        .flatMap(templateCatalog::findById)
        .map(PageModelTemplateView::model)
        .orElseGet(jsonUtils::emptyObjectNode);
        var now = Instant.now(clock);
    var reset = instance.resetToTemplate(defaultModel, instance.schemaVersion(), now, cmd.actorId());
    var saved = writer.save(reset);

    // Publish after-commit pour invalidation de cache (conform event_model.md §4 + §3.1)
    AfterCommit.run(() ->
        events.publish(new PageModelResetEvent(
            EventId.of(idGenerator.newUuid()),
            Instant.now(),
            saved.tenantId(),
            cmd.id(),
            cmd.actorId()
        ))
    );

    return mapper.toAdminDetailDto(saved);
  }
}
