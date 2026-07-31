package com.tchalanet.server.core.pagemodel.internal.infra.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PageModelAdminMapperTest {

  private static final UUID PAGE_MODEL_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID TENANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID TEMPLATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID ACTOR_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

  @Test
  void mapsTypedIdsToUuidStringsForDetailRoutes() {
    var instance =
        PageModelInstance.rehydrate(
            PAGE_MODEL_ID,
            TENANT_ID,
            "private.dashboard.tenant_admin",
            "private",
            "dashboard",
            PageModelStatus.PUBLISHED,
            3,
            JsonUtils.emptyObject(),
            TEMPLATE_ID,
            Instant.parse("2026-07-31T10:00:00Z"),
            Instant.parse("2026-07-31T10:00:00Z"),
            ACTOR_ID,
            ACTOR_ID,
            Instant.parse("2026-07-31T10:00:00Z"),
            null,
            null);

    var view = new PageModelAdminMapper().toAdminDetailDto(instance);

    assertThat(view.id()).isEqualTo(PAGE_MODEL_ID.toString());
    assertThat(view.tenantId()).isEqualTo(TENANT_ID.toString());
    assertThat(view.templateId()).isEqualTo(TEMPLATE_ID.toString());
    assertThat(view.createdBy()).isEqualTo(ACTOR_ID.toString());
    assertThat(view.updatedBy()).isEqualTo(ACTOR_ID.toString());
  }

  @Test
  void mapsMissingTemplateIdToNull() {
    var instance =
        PageModelInstance.rehydrate(
            PAGE_MODEL_ID,
            TENANT_ID,
            "private.dashboard.tenant_admin",
            "private",
            "dashboard",
            PageModelStatus.PUBLISHED,
            3,
            JsonUtils.emptyObject(),
            null,
            Instant.parse("2026-07-31T10:00:00Z"),
            Instant.parse("2026-07-31T10:00:00Z"),
            ACTOR_ID,
            ACTOR_ID,
            Instant.parse("2026-07-31T10:00:00Z"),
            null,
            null);

    assertThat(new PageModelAdminMapper().toAdminDetailDto(instance).templateId()).isNull();
  }
}
