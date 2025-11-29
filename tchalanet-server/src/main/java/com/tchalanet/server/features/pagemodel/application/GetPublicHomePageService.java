package com.tchalanet.server.features.pagemodel.application;

import com.tchalanet.server.features.pagemodel.application.dto.PublicHomeDynamicData;
import com.tchalanet.server.features.pagemodel.domain.model.PageModel;
import com.tchalanet.server.features.pagemodel.domain.ports.in.GetPublicHomePageUseCase;
import com.tchalanet.server.features.pagemodel.domain.ports.out.PageModelRepositoryPort;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPublicHomePageService implements GetPublicHomePageUseCase {

  private final PageModelRepositoryPort repositoryPort;
  // private final PlanReadModelPort planReadModelPort; // To fetch plans
  // private final GameReadModelPort gameReadModelPort; // To fetch games
  // private final DrawReadModelPort drawReadModelPort; // To fetch draws today and next draw
  // private final NewsReadModelPort newsReadModelPort; // To fetch news

  // pour V1, on utilise le tenant "platform" constant
  private static final UUID PLATFORM_TENANT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Override
  public EnrichedPageModel getPublicHome(String lang) {
    String normalized = normalizeLang(lang);
    PageModel pageModel =
        repositoryPort
            .findByTenantAndCodeAndLang(PLATFORM_TENANT_ID, "public_home", normalized)
            .orElseThrow(
                () ->
                    new IllegalStateException("No PageModel 'public_home' for lang=" + normalized));

    // Fetch dynamic data (placeholders for now)
    PublicHomeDynamicData dynamicData =
        new PublicHomeDynamicData(
            PLATFORM_TENANT_ID,
            Collections.emptyList(), // plans
            Collections.emptyList(), // games
            Collections.emptyList(), // drawsToday
            Collections.emptyMap(), // nextDraw
            Collections.emptyList() // news
            );

    // In a real implementation, you would call read model ports here:
    // dynamicData.plans = planReadModelPort.getPlansForTenant(PLATFORM_TENANT_ID);
    // dynamicData.games = gameReadModelPort.getGamesForTenant(PLATFORM_TENANT_ID);
    // dynamicData.drawsToday = drawReadModelPort.getDrawsToday(PLATFORM_TENANT_ID);
    // dynamicData.nextDraw = drawReadModelPort.getNextDraw(PLATFORM_TENANT_ID);
    // dynamicData.news = newsReadModelPort.getLatestNews();

    return new EnrichedPageModel(pageModel, dynamicData);
  }

  private String normalizeLang(String lang) {
    if (lang == null || lang.isBlank()) {
      return "fr";
    }
    return lang.toLowerCase(Locale.ROOT);
  }
}
