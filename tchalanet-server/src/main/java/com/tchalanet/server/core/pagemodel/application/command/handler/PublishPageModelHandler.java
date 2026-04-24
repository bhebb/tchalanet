package com.tchalanet.server.core.pagemodel.application.command.handler;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.core.pagemodel.application.command.model.PublishPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.port.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.port.PageModelWritePort;
import com.tchalanet.server.core.pagemodel.domain.policy.PublishPolicy;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PublishPageModelHandler {

  private final Clock clock;
  private final PageModelReadPort readPort;
  private final PageModelWritePort writePort;
  private final PublishPolicy publishPolicy;

  @Transactional
  public void handle(PublishPageModelCommand cmd) {
    var ctx = TchContext.get();
    var actorId = ctx.userUuid();
    var now = clock.instant();

    var toPublish = readPort.findById(cmd.id().uuid()).orElseThrow();

    // RLS-scoped list (current tenant)
    var currentPublished = readPort.findAllPublishedByLogicalId(toPublish.logicalId());

    var changed = publishPolicy.apply(toPublish, currentPublished, now, actorId);

    writePort.saveAll(changed);
  }
}
