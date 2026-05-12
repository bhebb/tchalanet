package com.tchalanet.server.core.haiti.internal.domain.lottery.service;

import com.tchalanet.server.common.contracts.haiti.HaitiResult;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionConfig;

public interface HaitiResultProjector {
  HaitiResult project(HaitiProjectionConfig config, ExternalPick pick);
}
