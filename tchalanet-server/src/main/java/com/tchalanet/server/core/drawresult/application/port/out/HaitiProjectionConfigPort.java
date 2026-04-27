package com.tchalanet.server.core.drawresult.application.port.out;

import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiProjectionConfig;

/**
 * Port that provides a Haiti projection configuration (used by draw handlers when projecting
 * results).
 */
public interface HaitiProjectionConfigPort {
  HaitiProjectionConfig getDefault();
}
