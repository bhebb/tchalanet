package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.application.command.model.DuplicatePageModelCommand;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReaderPort;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelWriterPort;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.infra.web.PageModelAdminMapper;
import com.tchalanet.server.core.pagemodel.infra.web.dto.PageModelAdminDetailDto;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

/**
 * Duplique un PageModel existant.
 * - Crée une copie DRAFT dans le même tenant que la source.
 * - newLogicalId / newSlug : utilisés si fournis, sinon suffixe "-copy" appliqué.
 * - L'ID est généré via IdGenerator (typed_ids §6).
 */
@UseCase
@RequiredArgsConstructor
public class DuplicatePageModelCommandHandler
    implements CommandHandler<DuplicatePageModelCommand, PageModelAdminDetailDto> {

  private final Clock clock;
  private final PageModelReaderPort reader;
  private final PageModelWriterPort writer;
  private final IdGenerator idGenerator;
  private final PageModelAdminMapper mapper;

  @Override
  @TchTx
  public PageModelAdminDetailDto handle(DuplicatePageModelCommand cmd) {
    var source = reader.findById(cmd.sourceId())
        .orElseThrow(() -> ProblemRest.notFound("pagemodel.not_found", cmd.sourceId()));

    String targetLogicalId = cmd.newLogicalId()
        .filter(s -> !s.isBlank())
        .orElseGet(() -> source.logicalId() + "-copy");

    String targetSlug = cmd.newSlug()
        .filter(s -> !s.isBlank())
        .orElseGet(() -> source.slug() != null ? source.slug() + "-copy" : null);

    var actorUuid = cmd.actorId() != null ? cmd.actorId().value() : null;
    var now = clock.instant();

    var copy = PageModelInstance.createDraft(
        PageModelId.of(idGenerator.newUuid()),
        source.tenantId(),      // même tenant que la source
        targetLogicalId,
        source.scope(),
        targetSlug,
        source.schemaVersion(),
        source.modelJson(),
        source.templateId().orElse(null),
        now,
        UserId.nullableOf(actorUuid)
    );

    var saved = writer.save(copy);
    return mapper.toAdminDetailDto(saved);
  }
}
