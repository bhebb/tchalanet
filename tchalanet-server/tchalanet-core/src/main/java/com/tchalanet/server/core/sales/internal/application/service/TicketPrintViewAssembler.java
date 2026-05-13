package com.tchalanet.server.core.sales.internal.application.service;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import com.tchalanet.server.core.sales.api.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.print.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.print.TicketPrintViewMapper;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TicketPrintViewAssembler implements TicketPrintReaderPort {

    private final DrawChannelCatalog drawChannelCatalog;
    private final TicketReaderPort ticketReader;
    private final DrawLookupPort drawReader;
    private final OutletReaderPort outletReader;
    private final SalesSessionReaderPort sessionReader;
    private final TicketPrintViewMapper mapper;

    @Override
    public Optional<TicketPrintView> findTicketPrintView(TicketId ticketId, Locale locale) {
        var ticket =
            ticketReader
                .findWithLinesById(ticketId)
                .orElse(null);
        if (ticket == null) {
            return Optional.empty();
        }

        var draw = drawReader.findById(DrawId.of(ticket.getDrawId().value())).orElse(null);
        var channelId = draw != null ? draw.drawChannelId() : null;
        Outlet outlet = resolveOutlet(ticket.getSessionId());
        var channel = drawChannelCatalog.findById(draw.tenantId(), channelId).orElse(null);
        return Optional.of(mapper.map(ticket, outlet, draw, channel, locale == null ? Locale.FRENCH : locale));
    }

    private Outlet resolveOutlet(SalesSessionId sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionReader
            .findById(sessionId)
            .flatMap(session -> outletReader.findById(session.outletId()))
            .orElse(null);
    }
}
