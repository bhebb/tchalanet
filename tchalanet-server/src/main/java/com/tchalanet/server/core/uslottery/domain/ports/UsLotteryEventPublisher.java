package com.tchalanet.server.core.uslottery.domain.ports;

import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import java.util.List;

/**
 * Port for publishing events related to the US Lottery domain. This decouples the domain logic from
 * the event publishing mechanism.
 */
public interface UsLotteryEventPublisher {

  /**
   * Publishes an event when a new set of lottery results has been fetched from an external
   * provider.
   *
   * @param latestDraws The list of latest draws fetched.
   */
  void publishLatestResults(List<LatestDraw> latestDraws);
}
