package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;

import java.util.List;
import java.util.Optional;


public interface TicketReaderPort {

    Optional<Ticket> findById(TicketId ticketId);

    Ticket getRequired(TicketId ticketId);

    Optional<Ticket> findByTicketCode(String ticketCode);

    Optional<Ticket> findByPublicCode(String publicCode);

    Optional<Ticket> findByVerificationCode(String verificationCode);

    List<Ticket> findByDrawId(DrawId drawId);
}
