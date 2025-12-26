package com.tchalanet.server.features.news.batch;

import com.tchalanet.server.features.news.config.NewsConfigProperties;
import com.tchalanet.server.features.news.shared.service.ExternalNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsRefreshScheduler {

  private final ExternalNewsService externalNewsService;
  private final NewsConfigProperties newsConfigProperties;

  /**
   * Tâche planifiée qui rafraîchit les news publiques selon la configuration globale
   * `tch.news.refresh.cron`.
   */
  @Scheduled(cron = "${tch.news.refresh.cron:0 0 */6 * * *}")
  public void refreshNews() {
    var cron = newsConfigProperties.getRefresh().getCron();
    log.info("Scheduled refresh of public news starting (cron={})", cron);
    externalNewsService.fetchSnapshot();
  }
}
