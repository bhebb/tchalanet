package com.tchalanet.server.platform.publiccontent.internal.scheduler;

import com.tchalanet.server.platform.publiccontent.internal.news.ExternalRssNewsService;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublicContentRefreshScheduler {

  private final ExternalRssNewsService externalRssService;
  private final PublicContentConfigProperties props;

  @Scheduled(cron = "${tch.news.refresh.cron:0 0 */6 * * *}")
  public void refreshExternalContent() {
    String cron = props.refresh() != null ? props.refresh().cron() : "default";
    log.info("publiccontent: scheduled external RSS refresh (cron={})", cron);
    externalRssService.refreshExternalSnapshot();
  }
}
