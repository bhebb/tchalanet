package com.tchalanet.server.core.pagemodel.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.api.command.PublishPageModelCommand;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelWritePort;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.internal.domain.policy.PublishPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublishPageModelCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
  private static final UserId ACTOR_ID =
      UserId.of(UUID.fromString("40000000-0000-0000-0000-000000000001"));

  @Test
  void publishesDraftAndWritesTheChangedInstance() {
    var pageModelId = PageModelId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    var draft = instance(pageModelId, "DRAFT");
    var reader = mock(PageModelReadPort.class);
    var writer = mock(PageModelWritePort.class);

    when(reader.findById(pageModelId)).thenReturn(Optional.of(draft));
    when(reader.findAllPublishedByLogicalId(draft.logicalId())).thenReturn(List.of());

    var handler =
        new PublishPageModelCommandHandler(
            Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC),
            reader,
            writer,
            new PublishPolicy());

    handler.handle(new PublishPageModelCommand(pageModelId, TENANT_ID, ACTOR_ID));

    assertThat(draft.status().name()).isEqualTo("PUBLISHED");
    verify(writer).saveAll(List.of(draft));
  }

  private static PageModelInstance instance(PageModelId id, String status) {
    return PageModelInstance.rehydrate(
        id.value(),
        TENANT_ID.value(),
        "private.dashboard.tenant_admin",
        "private",
        "dashboard",
        com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus.valueOf(status),
        2,
        com.tchalanet.server.common.json.utils.JsonUtils.emptyObject(),
        null,
        Instant.parse("2026-07-31T10:00:00Z"),
        Instant.parse("2026-07-31T10:00:00Z"),
        ACTOR_ID.value(),
        ACTOR_ID.value(),
        null,
        null,
        null);
  }
}
