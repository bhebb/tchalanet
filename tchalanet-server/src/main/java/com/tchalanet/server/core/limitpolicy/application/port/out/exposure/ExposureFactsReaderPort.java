package com.tchalanet.server.core.limitpolicy.application.port.out.exposure;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

public interface ExposureFactsReaderPort {
  LimitFactsSnapshot snapshot(LimitContext context);
}
