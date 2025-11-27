package com.tchalanet.server.uslottery.domain.events;

import com.tchalanet.server.uslottery.domain.dto.LatestDrawDto;
import java.util.List;

public record LatestUsLotteryResultsEvent(List<LatestDrawDto> results) {}
