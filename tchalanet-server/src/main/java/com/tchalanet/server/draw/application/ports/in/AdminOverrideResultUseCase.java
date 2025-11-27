package com.tchalanet.server.draw.application.ports.in;

import com.tchalanet.server.draw.web.dto.OverrideResultRequest;
import java.util.UUID;

public interface AdminOverrideResultUseCase {
  void overrideResult(UUID tenantId, UUID drawId, OverrideResultRequest req, UUID adminId);
}
