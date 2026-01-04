package com.tchalanet.server.core.draw.application.port.out;

import java.util.UUID;

public interface DrawApplyPort {
  enum ApplyOutcome {
    UPDATED,
    ALREADY_LINKED_OR_NOT_ELIGIBLE
  }

  ApplyOutcome attachResultAndMarkResulted(UUID drawId, UUID drawResultId, boolean force);
}
