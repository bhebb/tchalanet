package com.tchalanet.server.features.pagemodel.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.pagemodeltemplate.api.PageModelTemplateCatalog;
import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PageModelOnboardingServiceTest {

  @Test
  void seedDefaultsSkipsTemplateWhenPageModelAlreadyExists() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var templateId = PageModelTemplateId.of(UUID.randomUUID());
    var template =
        new PageModelTemplateView(
            templateId, "tpl-code", "private.dashboard.tenant_admin", "PRIVATE", "dashboard",
            "Admin Dashboard", null, null, null, null, 2, true, null, null, null, null);

    var catalog = mock(PageModelTemplateCatalog.class);
    var commandBus = mock(CommandBus.class);
    var queryBus = mock(QueryBus.class);

    when(catalog.findDefaultGlobalTemplates()).thenReturn(List.of(template));
    // queryBus.ask → returns a value (not null), meaning the page model already exists
    when(queryBus.ask(any())).thenReturn(new Object());

    var service = new PageModelOnboardingService(catalog, commandBus, queryBus, mock(JsonUtils.class));
    service.seedDefaults(tenantId);

    // commandBus must NOT have been called — template already exists
    verify(commandBus, never()).execute(any());
  }

  @Test
  void seedDefaultsSeedsTemplateWhenPageModelNotFound() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var templateId = PageModelTemplateId.of(UUID.randomUUID());
    var template =
        new PageModelTemplateView(
            templateId, "tpl-code", "private.dashboard.tenant_admin", "PRIVATE", "dashboard",
            "Admin Dashboard", null, null, null, null, 2, true, null, null, null, null);

    var catalog = mock(PageModelTemplateCatalog.class);
    var commandBus = mock(CommandBus.class);
    var queryBus = mock(QueryBus.class);

    when(catalog.findDefaultGlobalTemplates()).thenReturn(List.of(template));
    // queryBus.ask → throws TchNotFoundException, meaning page model does not exist yet
    when(queryBus.ask(any())).thenThrow(new TchNotFoundException("pagemodel.not_found", "Not found"));

    var service = new PageModelOnboardingService(catalog, commandBus, queryBus, mock(JsonUtils.class));
    service.seedDefaults(tenantId);

    // commandBus must have been called once to create the page model
    verify(commandBus).execute(any());
  }

  @Test
  void seedDefaultsContinuesToNextTemplateWhenSeedingFails() {
    var tenantId = TenantId.of(UUID.randomUUID());
    var templateId1 = PageModelTemplateId.of(UUID.randomUUID());
    var templateId2 = PageModelTemplateId.of(UUID.randomUUID());

    var failingTemplate =
        new PageModelTemplateView(
            templateId1, "tpl-1", "private.dashboard.failing", "PRIVATE", "dashboard",
            "Failing", null, null, null, null, 2, true, null, null, null, null);
    var successTemplate =
        new PageModelTemplateView(
            templateId2, "tpl-2", "private.dashboard.success", "PRIVATE", "dashboard",
            "Success", null, null, null, null, 2, true, null, null, null, null);

    var catalog = mock(PageModelTemplateCatalog.class);
    var commandBus = mock(CommandBus.class);
    var queryBus = mock(QueryBus.class);

    when(catalog.findDefaultGlobalTemplates()).thenReturn(List.of(failingTemplate, successTemplate));
    // Both templates: queryBus throws NotFound (both need seeding)
    when(queryBus.ask(any()))
        .thenThrow(new TchNotFoundException("pagemodel.not_found", "Not found"));
    // commandBus: first call throws, second succeeds
    when(commandBus.execute(any()))
        .thenThrow(new RuntimeException("Simulated seed failure"))
        .thenReturn(null);

    var service = new PageModelOnboardingService(catalog, commandBus, queryBus, mock(JsonUtils.class));
    // Should not throw — failures are caught and logged
    service.seedDefaults(tenantId);

    // Verify commandBus was called twice (once for each template)
    verify(commandBus, org.mockito.Mockito.times(2)).execute(any());
  }

  @Test
  void seedDefaultsForDefaultTenantCallsSeedWithFixedTenantId() {
    var catalog = mock(PageModelTemplateCatalog.class);
    var commandBus = mock(CommandBus.class);
    var queryBus = mock(QueryBus.class);

    when(catalog.findDefaultGlobalTemplates()).thenReturn(List.of());

    var service = new PageModelOnboardingService(catalog, commandBus, queryBus, mock(JsonUtils.class));
    service.seedDefaultsForDefaultTenant();

    verify(catalog).findDefaultGlobalTemplates();
    verify(commandBus, never()).execute(any());
  }
}
