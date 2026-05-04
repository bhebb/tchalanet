package com.tchalanet.server.core.sales.application.service;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintViewPort;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.print.TicketPrintViewMapper;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class TicketPrintViewAssembler implements TicketPrintViewPort {

    private final DrawChannelCatalog drawChannelCatalog;
    private final TicketReaderPort ticketReader;
    private final DrawLookupPort drawReader;
    private final OutletReaderPort outletReader;
    private final SalesSessionReaderPort sessionReader;
    private final TicketPrintViewMapper mapper;

    @Override
    public TicketPrintView getTicketPrintView(TicketId ticketId, Locale locale) {
        var ticket =
            ticketReader
                .findWithLinesById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        var draw = drawReader.findById(DrawId.of(ticket.getDrawId().value())).orElse(null);
        var channelId = draw != null ? draw.drawChannelId() : null;
        Outlet outlet = resolveOutlet(ticket.getSessionId());
        var channel = drawChannelCatalog.findById(draw.tenantId(), channelId).orElse(null);
        return mapper.map(ticket, outlet, draw, channel, locale == null ? Locale.FRENCH : locale);
    }

    private Outlet resolveOutlet(SessionId sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionReader
            .findById(sessionId)
            .flatMap(session -> outletReader.findById(session.outletId()))
            .orElse(null);
    }
}
