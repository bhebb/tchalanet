package com.tchalanet.server.core.draw.internal.domain.exception;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;

public class DrawResultNotFinalException extends RuntimeException {
  public DrawResultNotFinalException(DrawId drawId, DrawResultId drawResultId) {
    super("Draw " + drawId + " cannot be settled because its result " + drawResultId + " is not FINAL");
  }
}
