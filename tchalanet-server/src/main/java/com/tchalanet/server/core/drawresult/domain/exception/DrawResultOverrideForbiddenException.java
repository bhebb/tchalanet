package com.tchalanet.server.core.drawresult.domain.exception;

import com.tchalanet.server.common.types.id.DrawResultId;

public class DrawResultOverrideForbiddenException extends RuntimeException {
  public DrawResultOverrideForbiddenException(DrawResultId drawResultId) {
    super("Override forbidden for result " + drawResultId + " because at least one linked draw is already SETTLED");
  }
}
