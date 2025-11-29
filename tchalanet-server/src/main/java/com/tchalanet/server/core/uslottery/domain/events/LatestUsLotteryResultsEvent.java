package com.tchalanet.server.core.uslottery.domain.events;

import com.tchalanet.server.core.uslottery.domain.dto.LatestDrawDto;
import java.util.List;

public record LatestUsLotteryResultsEvent(List<LatestDrawDto> results) {}
