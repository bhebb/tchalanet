package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.json.utils.JsonUtils;
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
  private final JsonUtils json;

  @Override
  @TchTx
  public Integer handle(CreatePageTemplateUpdateNotificationsCommand command) {
    var affected = pageModels.findAllByTemplateId(command.templateId());
    var created = 0;
    for (var pageModel : affected) {
      classify(pageModel.schemaVersion(), command.newSchemaVersion());
      // todo add notification
      created++;
    }
    return created;
  }

  private static String classify(int currentVersion, int newVersion) {
    if (newVersion <= currentVersion) {
      return "PATCH";
    }
    if (newVersion == currentVersion + 1) {
      return "MINOR";
    }
    return "MAJOR";
  }
}
