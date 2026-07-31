package com.tchalanet.server.app.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.GlobalErrorHandler;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPagingArgumentResolver;
import com.tchalanet.server.core.pagemodel.api.query.ListPageModelsQuery;
import com.tchalanet.server.core.pagemodel.api.query.PageModelSummaryView;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelStatus;
import com.tchalanet.server.core.pagemodel.internal.infra.web.PageModelAdminController;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PageModelPlatformRouteTest {

  private static final UUID TENANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

  @Test
  void platformRouteListsPageModelsAndPreservesListFilters() throws Exception {
    var commandBus = mock(CommandBus.class);
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any()))
        .thenReturn(
            TchPage.of(
                List.of(
                    new PageModelSummaryView(
                        PageModelId.of(UUID.randomUUID()),
                        TENANT_ID,
                        "private.dashboard.tenant_admin",
                        "private",
                        "dashboard",
                        PageModelStatus.PUBLISHED,
                        3,
                        Instant.parse("2026-07-31T12:00:00Z"))),
                0,
                20,
                1,
                1,
                true,
                false,
                false));

    mvc(commandBus, queryBus)
        .perform(
            get("/platform/pagemodels")
                .param("tenantId", TENANT_ID.toString())
                .param("scope", "private")
                .param("logicalId", "private.dashboard.tenant_admin"))
        .andExpect(status().isOk());

    var captor = ArgumentCaptor.forClass(ListPageModelsQuery.class);
    verify(queryBus).ask(captor.capture());
    assertThat(captor.getValue().tenantId()).isPresent().contains(TenantId.of(TENANT_ID));
    assertThat(captor.getValue().scope()).contains("private");
    assertThat(captor.getValue().logicalId()).contains("private.dashboard.tenant_admin");
  }

  private static MockMvc mvc(CommandBus commandBus, QueryBus queryBus) {
    return MockMvcBuilders.standaloneSetup(
            new PageModelAdminController(commandBus, queryBus, mock(JsonUtils.class)))
        .setControllerAdvice(new GlobalErrorHandler())
        .setCustomArgumentResolvers(new TchPagingArgumentResolver())
        .build();
  }
}
