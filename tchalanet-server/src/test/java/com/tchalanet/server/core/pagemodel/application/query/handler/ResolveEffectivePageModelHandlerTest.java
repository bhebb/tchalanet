package com.tchalanet.server.core.pagemodel.application.query.handler;

import static com.tchalanet.server.common.constant.CommonConstants.DEFAULT_TENANT_UUID;
import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelTemplateLoaderPort;
import com.tchalanet.server.core.pagemodel.application.query.model.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ResolveEffectivePageModelHandlerTest {

  private static final String LOGICAL_ID = "public.home";

  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());

  @AfterEach
  void clearContext() {
    TchContext.clear();
  }

  @Test
  void currentTenantReadUsesAlreadyBoundContextWithoutTemporarySwitch() {
    var tenantId = UUID.randomUUID();
    var previous = TchRequestContext.startupTenant(tenantId, "http-request");
    TchContext.set(previous);

    var readPort = new RecordingReadPort(jsonUtils, Optional.of(docFor(tenantId, "current")));
    var handler = new ResolveEffectivePageModelQueryHandler(readPort, missingTemplateLoader(), jsonUtils);

    var doc = handler.handle(new ResolveEffectivePageModelQuery(Optional.of(TenantId.of(tenantId)), LOGICAL_ID));

    assertThat(doc.meta().id()).isEqualTo("current");
    assertThat(readPort.contextsSeen()).containsExactly(previous);
    assertThat(TchContext.currentOrNull()).isSameAs(previous);
  }

  @Test
  void defaultTenantFallbackRestoresOriginalContextBeforeProvidersRun() {
    var requestTenantId = UUID.randomUUID();
    var previous = TchRequestContext.startupTenant(requestTenantId, "http-request");
    TchContext.set(previous);

    var readPort =
        new RecordingReadPort(
            jsonUtils,
            Optional.empty(),
            Optional.of(docFor(DEFAULT_TENANT_UUID, "default")));
    var handler = new ResolveEffectivePageModelQueryHandler(readPort, missingTemplateLoader(), jsonUtils);

    var doc = handler.handle(new ResolveEffectivePageModelQuery(Optional.of(TenantId.of(requestTenantId)), LOGICAL_ID));

    assertThat(doc.meta().id()).isEqualTo("default");
    assertThat(readPort.contextsSeen()).hasSize(2);
    assertThat(readPort.contextsSeen().get(0)).isSameAs(previous);
    assertThat(readPort.contextsSeen().get(1).tenantIdSafe()).isEqualTo(TenantId.of(DEFAULT_TENANT_UUID));
    assertThat(TchContext.currentOrNull()).isSameAs(previous);
  }

  private PageModelTemplateLoaderPort missingTemplateLoader() {
    return logicalId -> new PageModelDoc(
        new PageModelDoc.Meta("resource", "public", "home", "public", 1, List.of("fr"), "fr"),
        null,
        null,
        null);
  }

  private PageModelInstance docFor(UUID tenantId, String id) {
    return PageModelInstance.rehydrate(
        UUID.randomUUID(),
        tenantId,
        LOGICAL_ID,
        "public",
        "home",
        com.tchalanet.server.core.pagemodel.domain.model.PageModelStatus.PUBLISHED,
        1,
        pageModelJson(id),
        null,
        Instant.parse("2026-05-05T00:00:00Z"),
        Instant.parse("2026-05-05T00:00:00Z"),
        null,
        null,
        Instant.parse("2026-05-05T00:00:00Z"),
        null,
        null);
  }

  private JsonNode pageModelJson(String id) {
    return jsonUtils.parse(
        """
        {
          "meta": {
            "id": "%s",
            "scope": "public",
            "slug": "home",
            "context": "public",
            "schema_version": 1,
            "langs": ["fr"],
            "default_lang": "fr"
          }
        }
        """
            .formatted(id));
  }

  private static final class RecordingReadPort implements PageModelReadPort {
    private final List<TchRequestContext> contextsSeen = new ArrayList<>();
    private final List<Optional<PageModelInstance>> responses;

    @SafeVarargs
    private RecordingReadPort(JsonUtils jsonUtils, Optional<PageModelInstance>... responses) {
      this.responses = new ArrayList<>(List.of(responses));
    }

    private List<TchRequestContext> contextsSeen() {
      return contextsSeen;
    }

    @Override
    public Optional<PageModelInstance> findById(PageModelId id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<PageModelInstance> findPublishedByLogicalId(String logicalId) {
      contextsSeen.add(TchContext.currentOrNull());
      if (responses.isEmpty()) {
        return Optional.empty();
      }
      return responses.removeFirst();
    }

    @Override
    public List<PageModelInstance> findAllPublishedByLogicalId(String logicalId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<PageModelInstance> findAllByTemplateId(PageModelTemplateId templateId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Page<PageModelInstance> search(
        Optional<TenantId> tenantId,
        Optional<String> scope,
        Optional<String> logicalId,
        Pageable pageable) {
      throw new UnsupportedOperationException();
    }
  }
}
