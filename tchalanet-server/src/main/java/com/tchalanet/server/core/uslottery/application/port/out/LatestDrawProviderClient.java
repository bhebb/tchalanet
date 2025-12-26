package com.tchalanet.server.core.uslottery.application.port.out;

import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import java.util.List;

public interface LatestDrawProviderClient {

  UsLotteryProvider provider();

  List<LatestDraw> fetchLatestDraws();
}
