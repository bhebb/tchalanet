package com.tchalanet.server.core.pagemodel.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.notification.application.command.model.CreateNotificationCommand;
import com.tchalanet.server.core.notification.domain.model.NotificationAudienceType;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.pagemodel.application.command.model.CreatePageTemplateUpdateNotificationsCommand;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReadPort;
import java.util.Map;
import java.util.Set;
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
      var tenantId = pageModel.tenantId();
      var compatibility = classify(pageModel.schemaVersion(), command.newSchemaVersion());
      var recommendedAction = "MAJOR".equals(compatibility) ? "REQUIRES_MIGRATION" : "CREATE_DRAFT";
      var payload =
          json.toJsonNode(
              Map.of(
                  "templateId", command.templateId().value().toString(),
                  "logicalId", command.logicalId(),
                  "schemaVersion", command.newSchemaVersion(),
                  "compatibility", compatibility,
                  "recommendedAction", recommendedAction));

      commandBus.send(
          new CreateNotificationCommand(
              tenantId,
              "PAGE_MODEL_TEMPLATE",
              command.templateId().value().toString(),
              dedupeKey(tenantId, command),
              NotificationAudienceType.ROLE,
              TENANT_ADMIN_ROLE,
              "MAJOR".equals(compatibility) ? NotificationSeverity.ERROR : NotificationSeverity.WARNING,
              NotificationKind.ACTION_REQUIRED,
              NotificationCategory.PAGE_MODEL,
              "notifications.page_model.template_update.title",
              "notifications.page_model.template_update.message",
              "Page model template update requires review",
              "A page model template changed and requires tenant-admin review.",
              payload,
              "PAGE_MODEL_TEMPLATE_UPDATE_REVIEW",
              "/admin/page-model-template-updates/" + command.logicalId(),
              null,
              Set.of(NotificationChannel.WEB)));
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

  private static String dedupeKey(TenantId tenantId, CreatePageTemplateUpdateNotificationsCommand command) {
    return "pagemodel-template-update:"
        + tenantId.value()
        + ":"
        + command.templateId().value()
        + ":"
        + command.logicalId()
        + ":"
        + command.newSchemaVersion();
  }
}
