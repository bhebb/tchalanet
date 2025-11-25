package com.tchalanet.server.draw.infra.scheduler;

import com.tchalanet.server.draw.domain.usecase.RefreshPublicDrawsCacheUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublicCacheRefreshScheduler {

  private final RefreshPublicDrawsCacheUseCase refresh;

  public PublicCacheRefreshScheduler(RefreshPublicDrawsCacheUseCase refresh) {
    this.refresh = refresh;
  }

  // every minute by default - tune later
  @Scheduled(cron = "0 */1 * * * *")
  public void refresh() {
    refresh.refreshAllTenants();
  }
}
