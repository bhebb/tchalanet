package com.tchalanet.server.features.ticketdelivery.app;

import com.tchalanet.server.features.ticketdelivery.model.DeliverTicketRequest;
import com.tchalanet.server.features.ticketdelivery.model.EdgeDeliveryPayload;
import com.tchalanet.server.features.ticketdelivery.model.TicketDeliveryStatus;

public interface EdgeTicketDeliveryGateway {
  TicketDeliveryStatus deliver(DeliverTicketRequest request, EdgeDeliveryPayload payload);
}
