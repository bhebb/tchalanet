package com.tchalanet.server.core.pagemodel.application.command.handler;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.core.pagemodel.application.command.model.UpsertPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.port.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.port.PageModelWritePort;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpsertPageModelHandler {

  private final Clock clock;
  private final PageModelReadPort readPort;
  private final PageModelWritePort writePort;

  @Transactional
  public PageModelInstance handle(UpsertPageModelCommand cmd) {
    var ctx = TchContext.get();
    UUID actorId = ctx.userUuid();

    // tenant: either cmd.tenantId or context tenant
    UUID tenantId = cmd.tenantId().map(t -> t.value()).orElse(ctx.tenantUuid());

    var now = clock.instant();

    var modelJson = cmd.modelJson();

    PageModelInstance inst;
    if (cmd.id().isEmpty()) {
      inst = PageModelInstance.createDraft(
          UUID.randomUUID(),
          tenantId,
          cmd.logicalId(),
          cmd.scope(),
          cmd.slug(),
          cmd.schemaVersion() == null ? 1 : cmd.schemaVersion(),
          modelJson,
          null,
          now,
          actorId
      );
    } else {
      PageModelId pid = cmd.id().get();
      inst = readPort.findById(pid.uuid()).orElseThrow();
      inst.applyUpsert(
          cmd.scope(),
          cmd.slug(),
          cmd.schemaVersion() == null ? inst.schemaVersion() : cmd.schemaVersion(),
          modelJson,
          null,
          now,
          actorId
      );
    }

    return writePort.save(inst);
  }
}
