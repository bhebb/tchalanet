package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketPrintReaderPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.mapper.TicketJpaMapper;
import com.tchalanet.server.core.sales.internal.infra.persistence.mapper.TicketPrintViewMapper;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketPrintHeaderViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TicketPrintProjectionReaderAdapter implements TicketPrintReaderPort {

    private final TicketPrintHeaderViewRepository headerRepository;
    private final TicketJpaRepository ticketRepository;
    private final TicketJpaMapper ticketMapper;
    private final TicketPrintViewMapper ticketPrintViewMapper;

    @Override
    @Transactional(readOnly = true)
    public TicketPrintView findPrintViewRequired(TicketId ticketId) {
        var header = headerRepository.getRequired(ticketId.value());
        var ticketEntity = ticketRepository.findWithLinesAndChargesById(ticketId.value())
            .orElseThrow(() -> new IllegalStateException("Ticket aggregate not found for print header: " + ticketId));
        var ticket = ticketMapper.toDomain(ticketEntity);
        return ticketPrintViewMapper.toPrintView(header, ticket);
    }
}
