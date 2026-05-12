package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pagemodel.api.command.ReplacePageModelFromTemplateCommand;
import com.tchalanet.server.core.pagemodel.internal.application.service.PageModelTemplateUpdateActionService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReplacePageModelFromTemplateCommandHandler
    implements CommandHandler<ReplacePageModelFromTemplateCommand, Boolean> {

  private final PageModelTemplateUpdateActionService service;

  @Override
  @TchTx
  public Boolean handle(ReplacePageModelFromTemplateCommand command) {
    service.applyTemplate(command.logicalId(), command.actorId());
    return true;
  }
}
