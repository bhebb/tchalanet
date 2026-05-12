package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pagemodel.api.command.MergePageModelWithTemplateCommand;
import com.tchalanet.server.core.pagemodel.internal.application.service.PageModelTemplateUpdateActionService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class MergePageModelWithTemplateCommandHandler
    implements CommandHandler<MergePageModelWithTemplateCommand, Boolean> {

  private final PageModelTemplateUpdateActionService service;

  @Override
  @TchTx
  public Boolean handle(MergePageModelWithTemplateCommand command) {
    service.applyTemplate(command.logicalId(), command.actorId());
    return true;
  }
}
