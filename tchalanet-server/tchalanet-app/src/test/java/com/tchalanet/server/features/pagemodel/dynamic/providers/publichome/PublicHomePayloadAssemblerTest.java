package com.tchalanet.server.features.pagemodel.dynamic.providers.publichome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.platform.publiccontent.api.PublicContentApi;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentItemView;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PublicHomePayloadAssemblerTest {

  private final PublicContentApi publicContentApi = mock(PublicContentApi.class);
  private final PlanCatalog planCatalog = mock(PlanCatalog.class);
  private final PublicHomePayloadAssembler assembler =
      new PublicHomePayloadAssembler(publicContentApi, planCatalog);

  @Nested
  @DisplayName("buildNews")
  class News {

    @Test
    @DisplayName("returns up to requested limit and maps fields")
    void respectsLimit() {
      when(publicContentApi.listPublicHomeNews(3)).thenReturn(generateItems(3));

      var payload = assembler.assemble(3, null);

      assertThat(payload.news()).hasSize(3);
      assertThat(payload.news().get(0).id()).isNotNull();
      assertThat(payload.news().get(0).title()).isNotNull();
      assertThat(payload.news().get(0).link()).isNotNull();
    }

    @Test
    @DisplayName("falls back to default 5 when limit is zero or negative")
    void defaultLimit() {
      when(publicContentApi.listPublicHomeNews(5)).thenReturn(generateItems(5));

      assertThat(assembler.assemble(0, null).news()).hasSize(5);
      assertThat(assembler.assemble(-7, null).news()).hasSize(5);
    }

    @Test
    @DisplayName("caps limit at MAX_NEWS_LIMIT (20)")
    void cappedLimit() {
      when(publicContentApi.listPublicHomeNews(20)).thenReturn(generateItems(20));

      assertThat(assembler.assemble(100, null).news()).hasSize(20);
    }

    @Test
    @DisplayName("calls publicContentApi exactly once per assemble")
    void singleRead() {
      when(publicContentApi.listPublicHomeNews(anyInt())).thenReturn(List.of());

      assembler.assemble(5, null);

      verify(publicContentApi, times(1)).listPublicHomeNews(5);
    }
  }

  @Nested
  @DisplayName("buildPlans")
  class Plans {

    @Test
    @DisplayName("maps PlanCatalog output to payload entries")
    void mapsPlans() {
      when(planCatalog.listActive()).thenReturn(List.of(samplePlan()));
      when(publicContentApi.listPublicHomeNews(anyInt())).thenReturn(List.of());

      var payload = assembler.assemble(5, null);

      assertThat(payload.plans()).hasSize(1);
      assertThat(payload.plans().get(0).value()).isEqualTo("demo");
      assertThat(payload.plans().get(0).name()).isEqualTo("Démo");
      assertThat(payload.plans().get(0).currency()).isEqualTo("USD");
      assertThat(payload.plans().get(0).isDefault()).isTrue();
    }
  }

  private static List<PublicContentItemView> generateItems(int count) {
    var list = new ArrayList<PublicContentItemView>(count);
    for (int i = 0; i < count; i++) {
      list.add(new PublicContentItemView(
          UUID.randomUUID(),
          "title-" + i,
          "content-" + i,
          null,
          "https://news/" + i,
          PublicContentSourceType.EXTERNAL_RSS,
          Instant.parse("2026-01-01T00:00:00Z")));
    }
    return list;
  }

  private static PlanView samplePlan() {
    return new PlanView(
        new PlanId(UUID.randomUUID()),
        "demo",
        "Démo",
        "Plan de démonstration",
        new BigDecimal("0.00"),
        "USD",
        "MONTHLY",
        null,
        null,
        true,
        true,
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));
  }
}
