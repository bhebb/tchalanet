package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.qr.QrRenderer;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.application.print.TicketVerificationUrlBuilder;
import com.tchalanet.server.core.sales.application.query.model.GetTicketQrPngQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class GetTicketQrPngQueryHandler implements QueryHandler<GetTicketQrPngQuery, byte[]> {

  private final TicketPrintReaderPort port;
  private final TicketVerificationUrlBuilder urlBuilder;
  private final QrRenderer qr;

  public GetTicketQrPngQueryHandler(
      TicketPrintReaderPort port,
      TicketVerificationUrlBuilder urlBuilder,
      @Qualifier("qrPngRenderer") QrRenderer qr) {
    this.port = port;
    this.urlBuilder = urlBuilder;
    this.qr = qr;
  }

  @Override
  public byte[] handle(GetTicketQrPngQuery q) {
    var view = port.getTicketPrintView(q.ticketId(), Locale.FRENCH);
    var url = urlBuilder.buildUrl(view.publicCode());
    return qr.render(url, new QrRenderer.QrRenderSpec(q.sizePx()));
  }
}
