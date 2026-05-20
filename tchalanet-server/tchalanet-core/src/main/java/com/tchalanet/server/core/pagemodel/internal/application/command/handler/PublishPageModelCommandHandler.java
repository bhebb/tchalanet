package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import com.tchalanet.server.core.pagemodel.api.command.PublishPageModelCommand;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelWritePort;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pagemodel.internal.domain.policy.PublishPolicy;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

// [Phase 2A-2] suppression de TchContext.get() — actorId lu depuis cmd.actorId() (analysis §BLOQUANT)
// [Phase 3A] @UseCase + VoidCommandHandler pour câblage CQRS (analysis §MAJEUR command_query_handlers.md §3.2)
@UseCase
@RequiredArgsConstructor
public class PublishPageModelCommandHandler implements VoidCommandHandler<PublishPageModelCommand> {

  private final Clock clock;
  private final PageModelReadPort readPort;
  private final PageModelWritePort writePort;
  private final PublishPolicy publishPolicy;

  @TchTx
  @Override
  public void handle(PublishPageModelCommand cmd) {
    // actorId provient de la commande, fourni par le controller via TchRequestContext
    var actorId = cmd.actorId() != null ? cmd.actorId().value() : null;
    var now = clock.instant();

    var toPublish = readPort.findById(cmd.id()).orElseThrow();

    // RLS-scoped list (current tenant)
    var currentPublished = readPort.findAllPublishedByLogicalId(toPublish.logicalId());

    var changed = publishPolicy.apply(toPublish, currentPublished, now, actorId);

    writePort.saveAll(changed);
  }
}
