package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.qr.QrRenderer;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.GetTicketQrPngByPublicCodeQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GetTicketQrPngByPublicCodeQueryHandler
    implements QueryHandler<GetTicketQrPngByPublicCodeQuery, byte[]> {

  private final TicketReaderPort lookup;
  private final QrRenderer qr;
  private final QrPayloadBuilder payloadBuilder;

  public GetTicketQrPngByPublicCodeQueryHandler(
      TicketReaderPort lookup,
      @Qualifier("qrPngRenderer") QrRenderer qr,
      QrPayloadBuilder payloadBuilder) {
    this.lookup = lookup;
    this.qr = qr;
    this.payloadBuilder = payloadBuilder;
  }

  @Override
  public byte[] handle(GetTicketQrPngByPublicCodeQuery q) {
    var code = q.publicCode().trim();

    lookup
        .findByPublicCode(code)
        .orElseThrow(() -> new IllegalArgumentException("TICKET_NOT_FOUND"));

    var payload = payloadBuilder.ticketVerifyUrl(code);
    return qr.render(payload, new QrRenderer.QrRenderSpec(q.sizePx()));
  }
}
