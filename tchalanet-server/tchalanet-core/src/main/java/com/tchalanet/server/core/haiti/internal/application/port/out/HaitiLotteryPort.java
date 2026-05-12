package com.tchalanet.server.core.haiti.internal.application.port.out;

import com.tchalanet.server.common.contracts.haiti.HaitiProjectionOutput;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionConfig;

public interface HaitiLotteryPort {

  HaitiProjectionOutput projectResult(ExternalPick externalPick, HaitiProjectionConfig config);
}
