package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;

public interface ExposureProjectorPort {
  void applyTicketPlaced(TicketPlacedEvent event);
}
