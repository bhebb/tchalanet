package com.tchalanet.server.core.uslottery.domain.ports.out;

import com.tchalanet.server.core.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.core.uslottery.domain.model.UsLotteryProvider;
import java.util.List;

public interface LatestDrawProviderClient {

  UsLotteryProvider provider();

  List<LatestDrawDto> fetchLatestDraws();
}
