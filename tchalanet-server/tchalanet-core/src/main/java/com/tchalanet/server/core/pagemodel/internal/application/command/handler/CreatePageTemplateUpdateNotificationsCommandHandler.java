package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pagemodel.api.command.CreatePageTemplateUpdateNotificationsCommand;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReadPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreatePageTemplateUpdateNotificationsCommandHandler
    implements CommandHandler<CreatePageTemplateUpdateNotificationsCommand, Integer> {

  private static final String TENANT_ADMIN_ROLE = "TENANT_ADMIN";

  private final PageModelReadPort pageModels;
  private final CommandBus commandBus;

  @Override
  @TchTx
  public Integer handle(CreatePageTemplateUpdateNotificationsCommand command) {
    var affected = pageModels.findAllByTemplateId(command.templateId());
    // TODO create one notification per page model, classified PATCH/MINOR/MAJOR by
    // comparing pageModel.schemaVersion() with command.newSchemaVersion().
    return affected.size();
  }
}
