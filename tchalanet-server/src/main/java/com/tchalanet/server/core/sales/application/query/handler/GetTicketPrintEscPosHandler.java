package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.print.escpos.EscPosBuilder;
import com.tchalanet.server.common.qr.QrRenderer;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.print.TicketReceiptFormatter;
import com.tchalanet.server.core.sales.application.query.model.GetTicketPrintEscPosQuery;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GetTicketPrintEscPosHandler
    implements QueryHandler<GetTicketPrintEscPosQuery, byte[]> {

  private final TicketReaderPort port;
  private final QrPayloadBuilder payloadBuilder;
  private final QrRenderer qr;
  private final EscPosBuilder escpos;
  private final TicketReceiptFormatter formatter;

  public GetTicketPrintEscPosHandler(
      TicketReaderPort port,
      QrPayloadBuilder payloadBuilder,
      @Qualifier("qrEscPosRenderer") QrRenderer qr,
      EscPosBuilder escpos,
      TicketReceiptFormatter formatter) {
    this.port = port;
    this.payloadBuilder = payloadBuilder;
    this.qr = qr;
    this.escpos = escpos;
    this.formatter = formatter;
  }

  @Override
  public byte[] handle(GetTicketPrintEscPosQuery q) {
    var t = port.getTicketPrintView(q.ticketId());
    var verifyUrl = payloadBuilder.ticketVerifyUrl(t.publicCode());

    var model = formatter.formatModel(t, verifyUrl);

    // Render spans with bold on/off
    var parts = new ArrayList<byte[]>();
    parts.add(escpos.init());
    parts.add(escpos.alignLeft());

    // Titre centré (optionnel)
    parts.add(escpos.alignCenter());
    parts.add(escpos.boldOn());
    parts.add(escpos.text(model.title()));
    parts.add(escpos.boldOff());
    parts.add(escpos.lf());
    parts.add(escpos.alignLeft());

    for (var line : model.lines()) {
      for (var sp : line.spans()) {
        parts.add(sp.bold() ? escpos.boldOn() : escpos.boldOff());
        parts.add(escpos.text(sp.text()));
      }
      parts.add(escpos.boldOff());
      parts.add(escpos.lf());
    }

    // QR
    byte[] qrBytes = qr.render(verifyUrl, new QrRenderer.QrRenderSpec(280));
    parts.add(escpos.alignCenter());
    parts.add(qrBytes);
    parts.add(escpos.alignLeft());
    parts.add(escpos.cut());

    return escpos.concat(parts.toArray(new byte[0][]));
  }
}
