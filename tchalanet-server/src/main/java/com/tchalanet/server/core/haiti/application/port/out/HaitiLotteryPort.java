package com.tchalanet.server.core.haiti.application.port.out;

import com.tchalanet.server.common.contracts.haiti.HaitiProjectionOutput;
import com.tchalanet.server.core.haiti.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionConfig;

public interface HaitiLotteryPort {

  HaitiProjectionOutput projectResult(ExternalPick externalPick, HaitiProjectionConfig config);
}
