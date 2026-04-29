package com.tchalanet.server.core.pagemodel.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pagemodel.application.command.model.ReplacePageModelFromTemplateCommand;
import com.tchalanet.server.core.pagemodel.application.service.PageModelTemplateUpdateActionService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReplacePageModelFromTemplateHandler
    implements CommandHandler<ReplacePageModelFromTemplateCommand, Boolean> {

  private final PageModelTemplateUpdateActionService service;

  @Override
  @TchTx
  public Boolean handle(ReplacePageModelFromTemplateCommand command) {
    service.applyTemplate(command.logicalId(), command.actorId());
    return true;
  }
}
