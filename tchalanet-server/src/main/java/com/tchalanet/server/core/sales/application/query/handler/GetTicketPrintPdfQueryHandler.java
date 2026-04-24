package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.print.pdf.TicketPdfBuilder;
import com.tchalanet.server.common.qr.QrRenderer;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.print.TicketReceiptFormatter;
import com.tchalanet.server.core.sales.application.query.model.GetTicketPrintPdfQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GetTicketPrintPdfQueryHandler implements QueryHandler<GetTicketPrintPdfQuery, byte[]> {

  private final TicketReaderPort port;
  private final QrPayloadBuilder payloadBuilder;
  private final QrRenderer qr;
  private final TicketPdfBuilder pdf;
  private final TicketReceiptFormatter formatter;

  public GetTicketPrintPdfQueryHandler(
      TicketReaderPort port,
      QrPayloadBuilder payloadBuilder,
      @Qualifier("qrPngRenderer") QrRenderer qr,
      TicketPdfBuilder pdf,
      @Qualifier("ticketReceiptFormatterPdf") TicketReceiptFormatter formatter) {
    this.port = port;
    this.payloadBuilder = payloadBuilder;
    this.qr = qr;
    this.pdf = pdf;
    this.formatter = formatter;
  }

  @Override
  public byte[] handle(GetTicketPrintPdfQuery q) {
    var t = port.getTicketPrintView(q.ticketId());

    var verifyUrl = payloadBuilder.ticketVerifyUrl(t.publicCode());
    var model = formatter.formatModel(t, verifyUrl);

    byte[] qrBytes = qr.render(verifyUrl, new QrRenderer.QrRenderSpec(300));
    return pdf.buildReceiptPdf(model, qrBytes);
  }
}
