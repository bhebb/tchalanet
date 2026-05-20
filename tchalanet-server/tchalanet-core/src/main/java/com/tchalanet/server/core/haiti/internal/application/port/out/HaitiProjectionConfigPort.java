package com.tchalanet.server.core.haiti.internal.application.port.out;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionConfig;

/**
 * Port that provides a Haiti projection configuration (used by draw handlers when projecting
 * results).
 */
public interface HaitiProjectionConfigPort {
  HaitiProjectionConfig getDefault();

  HaitiProjectionConfig resolve(JsonNode projectionCfg);
}
