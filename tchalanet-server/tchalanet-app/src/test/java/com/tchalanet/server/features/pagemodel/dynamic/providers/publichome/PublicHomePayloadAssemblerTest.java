package com.tchalanet.server.features.pagemodel.dynamic.providers.publichome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.plan.api.PlanCatalog;
import com.tchalanet.server.catalog.plan.api.PlanView;
import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.features.news.publicnews.PublicNewsService;
import com.tchalanet.server.features.news.shared.LotteryNewsModels.LotteryNewsArticle;
import com.tchalanet.server.features.news.shared.NewsStatus;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PublicHomePayloadAssemblerTest {

  private final PublicNewsService newsService = mock(PublicNewsService.class);
  private final PlanCatalog planCatalog = mock(PlanCatalog.class);
  private final PublicHomePayloadAssembler assembler =
      new PublicHomePayloadAssembler(newsService, planCatalog);

  @Nested
  @DisplayName("buildNews")
  class News {

    @Test
    @DisplayName("returns up to requested limit and maps fields")
    void respectsLimit() {
      when(newsService.listAll()).thenReturn(generateArticles(8));

      var payload = assembler.assemble(3, null);

      assertThat(payload.news()).hasSize(3);
      assertThat(payload.news().get(0))
          .containsEntry("id", "id-0")
          .containsEntry("title", "title-0")
          .containsEntry("link", "https://news/0");
    }

    @Test
    @DisplayName("falls back to default 5 when limit is zero or negative")
    void defaultLimit() {
      when(newsService.listAll()).thenReturn(generateArticles(10));

      assertThat(assembler.assemble(0, null).news()).hasSize(5);
      assertThat(assembler.assemble(-7, null).news()).hasSize(5);
    }

    @Test
    @DisplayName("caps limit at MAX_NEWS_LIMIT (20)")
    void cappedLimit() {
      when(newsService.listAll()).thenReturn(generateArticles(50));

      assertThat(assembler.assemble(100, null).news()).hasSize(20);
    }

    @Test
    @DisplayName("calls news service exactly once per assemble")
    void singleRead() {
      when(newsService.listAll()).thenReturn(List.of());

      assembler.assemble(5, null);

      verify(newsService, times(1)).listAll();
    }
  }

  @Nested
  @DisplayName("buildPlans")
  class Plans {

    @Test
    @DisplayName("maps PlanCatalog output to payload entries")
    void mapsPlans() {
      when(planCatalog.listActive()).thenReturn(List.of(samplePlan()));
      when(newsService.listAll()).thenReturn(List.of());

      var payload = assembler.assemble(5, null);

      assertThat(payload.plans()).hasSize(1);
      assertThat(payload.plans().get(0))
          .containsEntry("value", "demo")
          .containsEntry("name", "Démo")
          .containsEntry("price", new BigDecimal("0.00"))
          .containsEntry("currency", "USD")
          .containsEntry("isDefault", true);
    }
  }

  private static List<LotteryNewsArticle> generateArticles(int count) {
    var list = new java.util.ArrayList<LotteryNewsArticle>(count);
    for (int i = 0; i < count; i++) {
      list.add(new LotteryNewsArticle(
          "id-" + i,
          "lotterydaily",
          "title-" + i,
          URI.create("https://news/" + i),
          null,
          "author-" + i,
          Instant.parse("2026-01-01T00:00:00Z"),
          List.of("loto"),
          "snippet-" + i,
          "<p>content " + i + "</p>",
          NewsStatus.PUBLISHED));
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
