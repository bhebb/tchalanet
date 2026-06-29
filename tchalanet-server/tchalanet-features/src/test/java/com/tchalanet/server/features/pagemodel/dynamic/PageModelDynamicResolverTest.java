package com.tchalanet.server.features.pagemodel.dynamic;

import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageModelDynamicResolverTest {

  @AfterEach
  void tearDown() {
    ApiResponseContext.clear();
  }

  @Test
  void providerFailureAddsWidgetErrorAndTargetedApiNotice() {
    var resolver = new PageModelDynamicResolver(List.of(new FailingProvider()));

    var payload = resolver.resolve(pageModel(), "fr", null);

    assertThat(payload.errors()).singleElement().satisfies(error -> {
      assertThat(error.widgetId()).isEqualTo("dashboard.tenantAdmin.commission");
      assertThat(error.provider()).isEqualTo("tenant_admin_dashboard");
      assertThat(error.code()).isEqualTo("dashboard.commissions.unavailable");
    });

    assertThat(ApiResponseContext.get().getNotices()).singleElement().satisfies(notice -> {
      assertThat(notice.code()).isEqualTo("dashboard.commissions.unavailable");
      assertThat(notice.domain()).isEqualTo("features.pagemodel");
      assertThat(notice.severity()).isEqualTo(NoticeSeverity.WARN);
      assertThat(notice.message()).isEqualTo("Page section unavailable.");
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

  private static PageModelDoc pageModel() {
    return new PageModelDoc(
        new PageModelDoc.Meta(
            "private.dashboard.tenant_admin",
            "private",
            "dashboard",
            null,
            2,
            List.of("fr"),
            "fr"
        ),
        null,
        null,
        new PageModelDoc.Content(
            null,
            Map.of(
                "dashboard.tenantAdmin.commission",
                new PageModelDoc.WidgetConfig(
                    "DashboardCommissionWidget",
                    new PageModelDoc.WidgetBinding("dynamic", "tenant_admin_dashboard"),
                    Map.of()
                )
            )
        )
    );
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
        com.tchalanet.server.common.context.TchRequestContext ctx,
        PageModelResolutionContext resolutionContext
    ) {
      throw new PageModelDynamicProviderException(
          "dashboard.commissions.unavailable",
          "Provider details must stay diagnostic-only"
      );
    }

    @Override
    public String providerKey() {
      return "tenant_admin_dashboard";
    }
  }
}
