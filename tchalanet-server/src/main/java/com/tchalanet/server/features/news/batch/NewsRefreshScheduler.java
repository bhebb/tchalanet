package com.tchalanet.server.features.news.batch;

import com.tchalanet.server.features.news.application.ports.in.RefreshPublicNewsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsRefreshScheduler {

  private final RefreshPublicNewsUseCase refresh;

  @Value("${news.refresh.cron:0 0 */6 * * *}")
  private String newsRefreshCron;

  @Scheduled(cron = "${news.refresh.cron:0 0 */6 * * *}")
  public void refreshNews() {
    log.info("Scheduled refresh of public news starting (cron={})", newsRefreshCron);
    refresh.refreshNews();
  }
}
