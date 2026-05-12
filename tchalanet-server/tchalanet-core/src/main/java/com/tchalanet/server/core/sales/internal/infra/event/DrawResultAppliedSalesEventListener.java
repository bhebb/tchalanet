package com.tchalanet.server.core.sales.internal.infra.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DrawResultAppliedSalesEventListener {

  public void onDrawResultApplied(Object event) {
    log.debug("DrawResultAppliedSalesEventListener received event={}", event);
  }
}

