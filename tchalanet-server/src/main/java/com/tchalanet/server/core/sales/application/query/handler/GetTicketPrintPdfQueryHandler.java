package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.print.pdf.TicketPdfBuilder;
import com.tchalanet.server.common.qr.QrRenderer;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.application.print.TicketReceiptFormatter;
import com.tchalanet.server.core.sales.application.print.TicketVerificationUrlBuilder;
import com.tchalanet.server.core.sales.application.query.model.GetTicketPrintPdfQuery;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GetTicketPrintPdfQueryHandler implements QueryHandler<GetTicketPrintPdfQuery, byte[]> {

  private final TicketPrintReaderPort port;
  private final TchContextResolver contextResolver;
  private final TicketVerificationUrlBuilder urlBuilder;
  private final QrRenderer qr;
  private final TicketPdfBuilder pdf;
  private final TicketReceiptFormatter formatter;

  public GetTicketPrintPdfQueryHandler(
      TicketPrintReaderPort port,
      TchContextResolver contextResolver,
      TicketVerificationUrlBuilder urlBuilder,
      @Qualifier("qrPngRenderer") QrRenderer qr,
      TicketPdfBuilder pdf,
      @Qualifier("ticketReceiptFormatterPdf") TicketReceiptFormatter formatter) {
    this.port = port;
    this.contextResolver = contextResolver;
    this.urlBuilder = urlBuilder;
    this.qr = qr;
    this.pdf = pdf;
    this.formatter = formatter;
  }

  @Override
  public byte[] handle(GetTicketPrintPdfQuery q) {
    var t = port.getTicketPrintView(q.ticketId(), currentLocale());
    var verifyUrl = urlBuilder.buildUrl(t.publicCode());
    var model = formatter.formatModel(t, verifyUrl);
    byte[] qrBytes = qr.render(verifyUrl, new QrRenderer.QrRenderSpec(300));
    return pdf.buildReceiptPdf(model, qrBytes);
  }

  private Locale currentLocale() {
    var ctx = contextResolver.currentOrNull();
    return ctx == null || ctx.locale() == null ? Locale.FRENCH : ctx.locale();
  }
}
