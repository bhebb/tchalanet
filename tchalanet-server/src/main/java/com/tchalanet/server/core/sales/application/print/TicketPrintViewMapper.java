package com.tchalanet.server.core.sales.application.print;

import com.tchalanet.server.core.draw.application.print.DrawChannelLabelResolver;
import com.tchalanet.server.core.draw.application.print.DrawOccurrenceLabelResolver;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
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

  public TicketPrintView map(Ticket ticket, Draw draw, DrawChannel channel, Locale locale) {
    String channelCode = channel != null ? channel.code() : null;
    String channelLabel = channelLabelResolver.resolve(channel, locale);
    String whenLabel = occurrenceLabelResolver.resolve(draw, channel, locale);

    var lines = ticket.lines().stream()
        .map(l -> new TicketPrintLine(
            l.gameCode(),
            l.selection(),
            l.stake(),
            l.potentialPayout()))
        .collect(Collectors.toList());

    return new TicketPrintView(
        ticket.id().uuid(),
        ticket.ticketCode(),
        ticket.publicCode(),
        ticket.terminalId().uuid(),
        ticket.drawId().uuid(),
        ticket.createdAt(),
        ticket.totalAmount(),
        channelCode,
        channelLabel,
        whenLabel,
        lines
    );
  }
}
