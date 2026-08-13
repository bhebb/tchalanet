package com.tchalanet.server.features.pagemodel.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.NoticeKind;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.error.PageModelErrorCodes;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PageModelDynamicResolverTest {

  @AfterEach
  void tearDown() {
    ApiResponseContext.clear();
  }

  @Test
  void providerFailureAddsWidgetErrorAndTargetedApiNotice() {
    var resolver = new PageModelDynamicResolver(List.of(new FailingProvider()));

    var payload = resolver.resolve(pageModel(), "fr", null);

    assertThat(payload.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.widgetId()).isEqualTo("dashboard.tenantAdmin.commission");
              assertThat(error.provider()).isEqualTo("tenant_admin_dashboard");
              assertThat(error.code()).isEqualTo("pagemodel.widget.unavailable");
            });

    assertThat(ApiResponseContext.get().getNotices())
        .singleElement()
        .satisfies(
            notice -> {
              assertThat(notice.code()).isEqualTo("pagemodel.widget.unavailable");
              assertThat(notice.domain()).isEqualTo("features.pagemodel");
              assertThat(notice.severity()).isEqualTo(NoticeSeverity.WARN);
              assertThat(notice.kind()).isEqualTo(NoticeKind.DEGRADATION);
              assertThat(notice.message()).isNull();
              assertThat(notice.meta())
                  .containsEntry("surface", "section")
                  .containsEntry("placement", "top")
                  .containsEntry("target", "dashboard.tenantAdmin.commission")
                  .containsEntry("source", "dashboard.tenantAdmin.commission")
                  .containsEntry("service", "tenant_admin_dashboard")
                  .containsEntry("operation", "loadWidget")
                  .containsKey("errorId");
            });
  }

  @Test
  void allErrorCodesRegistryIsNonEmpty() {
    assertThat(PageModelErrorCodes.all()).isNotEmpty();
  }

  @Test
  void providerSuccessPopulatesWidgetWithNoErrors() {
    var resolver = new PageModelDynamicResolver(List.of(new SuccessProvider("the-data")));

    var payload = resolver.resolve(pageModel(), "fr", null);

    assertThat(payload.errors()).isEmpty();
    assertThat(payload.widgets()).containsEntry("dashboard.tenantAdmin.commission", "the-data");
  }

  @Test
  void noMatchingProviderAddsNoProviderError() {
    var resolver = new PageModelDynamicResolver(List.of());

    var payload = resolver.resolve(pageModel(), "fr", null);

    assertThat(payload.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.widgetId()).isEqualTo("dashboard.tenantAdmin.commission");
              assertThat(error.provider()).isEqualTo("resolver");
              assertThat(error.code()).isEqualTo("pagemodel.widget.no_provider");
            });
    assertThat(payload.widgets()).isEmpty();
  }

  @Test
  void nullDocReturnsEmptyPayload() {
    var resolver = new PageModelDynamicResolver(List.of());

    var payload = resolver.resolve(null, "fr", null);

    assertThat(payload.errors()).isEmpty();
    assertThat(payload.widgets()).isEmpty();
  }

  @Test
  void uncheckedExceptionFromProviderAddsWidgetUnavailableError() {
    var resolver = new PageModelDynamicResolver(List.of(new ThrowingProvider()));

    var payload = resolver.resolve(pageModel(), "fr", null);

    assertThat(payload.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.widgetId()).isEqualTo("dashboard.tenantAdmin.commission");
              assertThat(error.code()).isEqualTo("pagemodel.widget.unavailable");
            });
    assertThat(payload.widgets()).isEmpty();
  }

  private static PageModelDoc pageModel() {
    return new PageModelDoc(
        new PageModelDoc.Meta(
            "private.dashboard.tenant_admin", "private", "dashboard", null, 2, List.of("fr"), "fr"),
        null,
        null,
        new PageModelDoc.Content(
            null,
            Map.of(
                "dashboard.tenantAdmin.commission",
                new PageModelDoc.WidgetConfig(
                    "DashboardCommissionWidget",
                    new PageModelDoc.WidgetBinding("dynamic", "tenant_admin_dashboard"),
                    Map.of()))));
  }

  private static final class FailingProvider implements PageModelDynamicProvider {
    @Override
    public boolean supports(String logicalId, String widgetType, String source) {
      return "tenant_admin_dashboard".equals(source);
    }

    @Override
    public Object load(
        PageModelDoc pageModel,
        String widgetId,
        PageModelDoc.WidgetConfig widgetConfig,
        String lang,
        TchRequestContext ctx,
        PageModelResolutionContext resolutionContext) {
      throw new PageModelDynamicProviderException(
          "dashboard.commissions.unavailable", "Provider details must stay diagnostic-only");
    }

    @Override
    public String providerKey() {
      return "tenant_admin_dashboard";
    }
  }

  private static final class SuccessProvider implements PageModelDynamicProvider {
    private final Object result;

    SuccessProvider(Object result) {
      this.result = result;
    }

    @Override
    public boolean supports(String logicalId, String widgetType, String source) {
      return "tenant_admin_dashboard".equals(source);
    }

    @Override
    public Object load(
        PageModelDoc pageModel,
        String widgetId,
        PageModelDoc.WidgetConfig widgetConfig,
        String lang,
        TchRequestContext ctx,
        PageModelResolutionContext resolutionContext) {
      return result;
    }

    @Override
    public String providerKey() {
      return "tenant_admin_dashboard";
    }
  }

  private static final class ThrowingProvider implements PageModelDynamicProvider {
    @Override
    public boolean supports(String logicalId, String widgetType, String source) {
      return "tenant_admin_dashboard".equals(source);
    }

    @Override
    public Object load(
        PageModelDoc pageModel,
        String widgetId,
        PageModelDoc.WidgetConfig widgetConfig,
        String lang,
        TchRequestContext ctx,
        PageModelResolutionContext resolutionContext) {
      throw new RuntimeException("Simulated infrastructure failure");
    }

    @Override
    public String providerKey() {
      return "tenant_admin_dashboard";
    }
  }
}
