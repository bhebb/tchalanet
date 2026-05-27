package com.tchalanet.server.features.pagemodel.dynamic.providers.publichome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PublicHomeProviderTest {

  private final PublicHomePayloadAssembler assembler = mock(PublicHomePayloadAssembler.class);
  private final PublicHomeProvider provider = new PublicHomeProvider(assembler);

  @Nested
  @DisplayName("supports")
  class Supports {

    @Test
    @DisplayName("matches source public_home")
    void matchesSource() {
      assertThat(provider.supports("public.home", "NewsTickerWidget", "public_home")).isTrue();
      assertThat(provider.supports("public.home", "NewsTickerWidget", "other_source")).isFalse();
    }
  }

  @Nested
  @DisplayName("load — dispatch by widgetId")
  class Dispatch {

    @Test
    @DisplayName("home.news returns items from payload.news()")
    void newsWidget() {
      var news = List.<Map<String, Object>>of(Map.of("id", "1", "title", "n1"));
      when(assembler.assemble(anyInt(), any())).thenReturn(
          new PublicHomePayloadAssembler.Payload(news, List.of()));

      Object result = provider.load(
          null, "home.news",
          widgetConfigWithLimit(7), "fr", null, new PageModelResolutionContext());

      assertThat(result).isInstanceOf(Map.class);
      assertThat(((Map<?, ?>) result).get("items")).isEqualTo(news);
    }

    @Test
    @DisplayName("home.plans returns plans from payload.plans()")
    void plansWidget() {
      var plans = List.<Map<String, Object>>of(Map.of("value", "demo"));
      when(assembler.assemble(anyInt(), any())).thenReturn(
          new PublicHomePayloadAssembler.Payload(List.of(), plans));

      Object result = provider.load(
          null, "home.plans",
          widgetConfigWithLimit(5), "fr", null, new PageModelResolutionContext());

      assertThat(((Map<?, ?>) result).get("plans")).isEqualTo(plans);
    }

    @Test
    @DisplayName("unknown widgetId raises PUBLIC_HOME_UNKNOWN_WIDGET")
    void unknownWidget() {
      when(assembler.assemble(anyInt(), any())).thenReturn(
          new PublicHomePayloadAssembler.Payload(List.of(), List.of()));

      assertThatThrownBy(() -> provider.load(
          null, "home.hero",
          widgetConfigWithLimit(5), "fr", null, new PageModelResolutionContext()))
          .isInstanceOfSatisfying(PageModelDynamicProviderException.class,
              e -> assertThat(e.code()).isEqualTo("PUBLIC_HOME_UNKNOWN_WIDGET"));
    }
  }

  @Nested
  @DisplayName("memoization via PageModelResolutionContext")
  class Memo {

    @Test
    @DisplayName("two widgets sharing the same limit reuse a single assembly")
    void sharedAssembly() {
      when(assembler.assemble(anyInt(), any())).thenReturn(
          new PublicHomePayloadAssembler.Payload(List.of(), List.of()));

      var ctx = new PageModelResolutionContext();
      provider.load(null, "home.news", widgetConfigWithLimit(5), "fr", null, ctx);
      provider.load(null, "home.plans", widgetConfigWithLimit(5), "fr", null, ctx);

      verify(assembler, times(1)).assemble(anyInt(), any());
    }

    @Test
    @DisplayName("two widgets with different limits trigger two assemblies")
    void distinctLimits() {
      when(assembler.assemble(anyInt(), any())).thenReturn(
          new PublicHomePayloadAssembler.Payload(List.of(), List.of()));

      var ctx = new PageModelResolutionContext();
      provider.load(null, "home.news", widgetConfigWithLimit(3), "fr", null, ctx);
      provider.load(null, "home.news", widgetConfigWithLimit(10), "fr", null, ctx);

      verify(assembler, times(2)).assemble(anyInt(), any());
    }
  }

  private static PageModelDoc.WidgetConfig widgetConfigWithLimit(int limit) {
    return new PageModelDoc.WidgetConfig(
        "NewsTickerWidget",
        new PageModelDoc.WidgetBinding("dynamic", "public_home"),
        Map.of("limit", limit));
  }
}
