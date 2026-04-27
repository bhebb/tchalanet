package com.tchalanet.server.core.uslottery.infra.external;

import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.application.port.out.ProviderDrawQuery;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery.providers.tn",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class TnLatestDrawProviderClient implements UsLotteryProviderClient {

  @Override
  public UsLotteryProvider provider() {
    return UsLotteryProvider.TN;
  }

  @Override
  public List<LatestDraw> fetchDraws(ProviderDrawQuery query) {
    log.info("test TN");
    return List.of();
  }
}
