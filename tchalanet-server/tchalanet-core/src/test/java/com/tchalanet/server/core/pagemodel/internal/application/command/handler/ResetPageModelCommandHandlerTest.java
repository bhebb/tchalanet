package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateLevel;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.api.command.ResetPageModelCommand;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReaderPort;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelWriterPort;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.internal.infra.web.PageModelAdminMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ResetPageModelCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final PageModelId PAGE_MODEL_ID =
      PageModelId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
  private static final PageModelTemplateId TEMPLATE_ID =
      PageModelTemplateId.of(UUID.fromString("30000000-0000-0000-0000-000000000001"));
  private static final UserId ACTOR_ID =
      UserId.of(UUID.fromString("40000000-0000-0000-0000-000000000001"));

  @Test
  void resetRestoresTheTemplateSchemaVersionAlongsideItsModel() {
    var reader = mock(PageModelReaderPort.class);
    var writer = mock(PageModelWriterPort.class);
    var templateCatalog = mock(PageModelTemplateCatalog.class);
    var mapper = mock(PageModelAdminMapper.class);
    var events = mock(DomainEventPublisher.class);
    var idGenerator = mock(IdGenerator.class);
    var jsonUtils = mock(JsonUtils.class);
    var model = JsonMapper.builder().build().createObjectNode().put("version", 3);
    var instance =
        PageModelInstance.rehydrate(
            PAGE_MODEL_ID.value(),
            TENANT_ID.value(),
            "private.dashboard.tenant_admin",
            "private",
            "dashboard",
            com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus.PUBLISHED,
            2,
            JsonMapper.builder().build().createObjectNode().put("version", 2),
            TEMPLATE_ID.value(),
            Instant.parse("2026-07-31T10:00:00Z"),
            Instant.parse("2026-07-31T10:00:00Z"),
            ACTOR_ID.value(),
            ACTOR_ID.value(),
            Instant.parse("2026-07-31T10:00:00Z"),
            null,
            null);
    var template =
        new PageModelTemplateView(
            TEMPLATE_ID,
            "private.dashboard.tenant_admin.template",
            "private.dashboard.tenant_admin",
            "private",
            "dashboard",
            "Tenant admin dashboard",
            "Tenant admin dashboard",
            "Dashboard template",
            JsonUtils.emptyObject(),
            model,
            3,
            true,
            PageModelTemplateLevel.GLOBAL,
            null,
            null,
            null);

    when(reader.findById(PAGE_MODEL_ID)).thenReturn(Optional.of(instance));
    when(templateCatalog.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
    when(writer.save(instance)).thenReturn(instance);

    var handler =
        new ResetPageModelCommandHandler(
            reader,
            writer,
            templateCatalog,
            mapper,
            events,
            idGenerator,
            jsonUtils,
            Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC));

    handler.handle(new ResetPageModelCommand(PAGE_MODEL_ID, ACTOR_ID));

    assertThat(instance.schemaVersion()).isEqualTo(3);
    assertThat(instance.modelJson()).isEqualTo(model);
  }

  @Test
  void resetFindsTheTemplateByLogicalIdForLegacyInstancesWithoutTemplateId() {
    var reader = mock(PageModelReaderPort.class);
    var writer = mock(PageModelWriterPort.class);
    var templateCatalog = mock(PageModelTemplateCatalog.class);
    var mapper = mock(PageModelAdminMapper.class);
    var instance =
        PageModelInstance.rehydrate(
            PAGE_MODEL_ID.value(),
            TENANT_ID.value(),
            "private.dashboard.tenant_admin",
            "private",
            "dashboard",
            com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus.PUBLISHED,
            2,
            JsonUtils.emptyObject(),
            null,
            Instant.parse("2026-07-31T10:00:00Z"),
            Instant.parse("2026-07-31T10:00:00Z"),
            ACTOR_ID.value(),
            ACTOR_ID.value(),
            Instant.parse("2026-07-31T10:00:00Z"),
            null,
            null);
    var model = JsonMapper.builder().build().createObjectNode().put("version", 3);
    var template =
        new PageModelTemplateView(
            TEMPLATE_ID,
            "private.dashboard.tenant_admin.template",
            "private.dashboard.tenant_admin",
            "private",
            "dashboard",
            "Tenant admin dashboard",
            "Tenant admin dashboard",
            "Dashboard template",
            JsonUtils.emptyObject(),
            model,
            3,
            true,
            PageModelTemplateLevel.GLOBAL,
            null,
            null,
            null);

    when(reader.findById(PAGE_MODEL_ID)).thenReturn(Optional.of(instance));
    when(templateCatalog.findByLogicalId(instance.logicalId())).thenReturn(Optional.of(template));
    when(writer.save(instance)).thenReturn(instance);

    var handler =
        new ResetPageModelCommandHandler(
            reader,
            writer,
            templateCatalog,
            mapper,
            mock(DomainEventPublisher.class),
            mock(IdGenerator.class),
            mock(JsonUtils.class),
            Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC));

    handler.handle(new ResetPageModelCommand(PAGE_MODEL_ID, ACTOR_ID));

    assertThat(instance.schemaVersion()).isEqualTo(3);
    assertThat(instance.modelJson()).isEqualTo(model);
  }
}
