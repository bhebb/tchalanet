package com.tchalanet.server.core.sales.internal.application.print;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelDisplayFormatter;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.core.draw.internal.domain.model.Draw;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import com.tchalanet.server.core.sales.internal.application.formatter.DrawLabelFormat;
import com.tchalanet.server.core.sales.internal.application.formatter.TicketDrawLabelFormatter;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintLine;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintView;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class TicketPrintViewMapper {

    private final DrawChannelDisplayFormatter channelDisplayFormatter;
    private final TicketDrawLabelFormatter ticketDrawLabelFormatter;

    public TicketPrintViewMapper(
        DrawChannelDisplayFormatter channelDisplayFormatter,
        TicketDrawLabelFormatter ticketDrawLabelFormatter) {
        this.channelDisplayFormatter = channelDisplayFormatter;
        this.ticketDrawLabelFormatter = ticketDrawLabelFormatter;
    }

    public TicketPrintView map(
        Ticket ticket, Outlet outlet, Draw draw, DrawChannelView channel, Locale locale) {

        String channelCode = channel != null ? channel.code() : null;
        String channelLabel = channelDisplayFormatter.resolve(channel, locale);
        String whenLabel = ticketDrawLabelFormatter.format(draw.drawDate(), channel.drawTime(), channel.timezone(),
            draw.scheduledAt(), Locale.US, DrawLabelFormat.TICKET_SHORT);

        var lines =
            ticket.getLines().stream()
                .map(
                    l ->
                        new TicketPrintLine(
                            // persist/print layer expects a String code; use enum name when domain stores GameCode
                            l.gameCode() == null ? null : l.gameCode().name(),
                            l.betType(),
                            l.betOption(),
                            l.selection(),
                            l.stake(),
                            l.potentialPayout()))
                .collect(Collectors.toList());

        return new TicketPrintView(
            ticket.getId().value(),
            ticket.getTicketCode(),
            ticket.getPublicCode(),
            ticket.getTerminalId().value(),
            ticket.getDrawId().value(),
            ticket.getCreatedAt(),
            ticket.getTotalAmount(),
            outlet == null ? null : outlet.name(),
            channelCode,
            channelLabel,
            whenLabel,
            lines);
    }
}
