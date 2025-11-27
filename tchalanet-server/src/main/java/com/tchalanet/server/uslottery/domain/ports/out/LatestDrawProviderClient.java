package com.tchalanet.server.uslottery.domain.ports.out;

import com.tchalanet.server.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.uslottery.domain.model.UsLotteryProvider;
import java.util.List;

public interface LatestDrawProviderClient {

  UsLotteryProvider provider();

  List<LatestDrawDto> fetchLatestDraws();
}
