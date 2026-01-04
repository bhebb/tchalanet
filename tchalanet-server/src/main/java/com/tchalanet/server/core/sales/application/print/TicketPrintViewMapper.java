package com.tchalanet.server.core.sales.application.print;

import com.tchalanet.server.core.draw.application.print.DrawChannelLabelResolver;
import com.tchalanet.server.core.draw.application.print.DrawOccurrenceLabelResolver;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintLine;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TicketPrintViewMapper {

  private final DrawChannelLabelResolver channelLabelResolver;
  private final DrawOccurrenceLabelResolver occurrenceLabelResolver;

  public TicketPrintViewMapper(
      DrawChannelLabelResolver channelLabelResolver,
      DrawOccurrenceLabelResolver occurrenceLabelResolver) {
    this.channelLabelResolver = channelLabelResolver;
    this.occurrenceLabelResolver = occurrenceLabelResolver;
  }

  public TicketPrintView map(
      Ticket ticket, Outlet outlet, Draw draw, DrawChannel channel, Locale locale) {
    String channelCode = channel != null ? channel.code() : null;
    String channelLabel = channelLabelResolver.resolve(channel, locale);
    String whenLabel = occurrenceLabelResolver.resolve(draw, channel, locale);

    var lines =
        ticket.getLines().stream()
            .map(
                l ->
                    new TicketPrintLine(
                        l.gameCode(), l.selection(), l.stake(), l.potentialPayout()))
            .collect(Collectors.toList());

    return new TicketPrintView(
        ticket.getId().uuid(),
        ticket.getTicketCode(),
        ticket.getPublicCode(),
        ticket.getTerminalId().uuid(),
        ticket.getDrawId().uuid(),
        ticket.getCreatedAt(),
        ticket.getTotalAmount(),
        outlet.name(),
        channelCode,
        channelLabel,
        whenLabel,
        lines);
  }
}
