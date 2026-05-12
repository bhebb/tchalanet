package com.tchalanet.server.core.limitpolicy.internal.application.port.out.exposure;

import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitFactsSnapshot;

public interface ExposureFactsReaderPort {
  LimitFactsSnapshot snapshot(LimitContext context);
}
