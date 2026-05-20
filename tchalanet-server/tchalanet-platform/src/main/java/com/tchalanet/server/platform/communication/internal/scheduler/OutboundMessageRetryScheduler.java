package com.tchalanet.server.platform.communication.internal.scheduler;

import com.tchalanet.server.platform.communication.internal.service.OutboundMessageDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboundMessageRetryScheduler {

  private final OutboundMessageDispatcher dispatcher;

  @Scheduled(fixedDelayString = "${tch.communication.dispatcher.fixed-delay:PT30S}")
  public void dispatchDueMessages() {
    var dispatched = dispatcher.dispatchDueMessages();
    if (dispatched > 0) {
      log.debug("Dispatched outbound communication messages count={}", dispatched);
    }
  }
}
