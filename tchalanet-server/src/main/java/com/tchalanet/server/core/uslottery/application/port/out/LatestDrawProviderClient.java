package com.tchalanet.server.core.uslottery.application.port.out;

import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.domain.model.UsLotteryProvider;

import java.util.List;

public interface LatestDrawProviderClient {

    UsLotteryProvider provider();

    List<LatestDraw> fetchLatestDraws();
}
