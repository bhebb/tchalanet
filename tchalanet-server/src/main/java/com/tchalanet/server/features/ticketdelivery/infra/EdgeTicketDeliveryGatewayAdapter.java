package com.tchalanet.server.features.ticketdelivery.infra;

import com.tchalanet.server.features.ticketdelivery.app.EdgeDeliveryPayload;
import com.tchalanet.server.features.ticketdelivery.app.EdgeTicketDeliveryGateway;
import com.tchalanet.server.features.ticketdelivery.model.DeliverTicketRequest;
import com.tchalanet.server.features.ticketdelivery.model.TicketDeliveryStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class EdgeTicketDeliveryGatewayAdapter implements EdgeTicketDeliveryGateway {

  private final RestClient client;
  private final TicketDeliveryProperties props;

  public EdgeTicketDeliveryGatewayAdapter(
      @Qualifier("edgeDeliveryRestClient") RestClient client,
      TicketDeliveryProperties props) {
    this.client = client;
    this.props = props;
  }

  @Override
  public TicketDeliveryStatus deliver(DeliverTicketRequest request, EdgeDeliveryPayload payload) {
    if (!props.enabled()) {
      log.info("Ticket delivery disabled — skipping. requestId={}", payload.requestId());
      return TicketDeliveryStatus.ACCEPTED;
    }

    try {
      var response = client.post()
          .uri(props.edgeDeliveryPath())
          .body(payload)
          .retrieve()
          .onStatus(HttpStatusCode::isError, (req, res) -> {
            throw new RestClientException("Edge delivery error: " + res.getStatusCode());
          })
          .toBodilessEntity();

      log.info("Ticket delivery accepted: requestId={} channel={}", payload.requestId(), payload.channel());
      return TicketDeliveryStatus.ACCEPTED;
    } catch (RestClientException e) {
      log.error("Ticket delivery failed: requestId={} error={}", payload.requestId(), e.getMessage());
      return TicketDeliveryStatus.FAILED;
    }
  }
}
