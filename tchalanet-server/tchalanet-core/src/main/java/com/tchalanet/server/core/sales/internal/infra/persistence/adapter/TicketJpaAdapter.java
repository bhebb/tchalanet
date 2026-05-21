package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.infra.persistence.mapper.TicketJpaMapper;
import com.tchalanet.server.core.sales.internal.infra.persistence.mapper.TicketAggregateMutator;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TicketJpaAdapter implements TicketReaderPort, TicketWriterPort {

    private final TicketJpaRepository ticketRepository;
    private final TicketJpaMapper mapper;
    private final TicketAggregateMutator mutator;

    @Override
    @Transactional
    public Ticket save(Ticket ticket) {
        var ticketId = ticket.identity().id().value();
        var existing = ticketRepository.findWithLinesById(ticketId);
        if (existing.isEmpty()) {
            var entity = mapper.toEntity(ticket);
            return mapper.toDomain(ticketRepository.save(entity));
        }

        var managed = existing.get();
        ticketRepository.findWithChargesById(ticketId);
        mutator.applyTo(managed, ticket);
        return mapper.toDomain(managed);
    }

    @Override
    @Transactional
    public void flushPending() {
        ticketRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findById(TicketId ticketId) {
        var entity = ticketRepository.findWithLinesById(ticketId.value());
        entity.ifPresent(ticket -> ticketRepository.findWithChargesById(ticket.getId()));
        return entity.map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getRequired(TicketId ticketId) {
        return findById(ticketId)
            .orElseThrow(() -> ProblemRest.notFound("ticket.not_found", ticketId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findByTicketCode(String ticketCode) {
        return ticketRepository.findWithLinesByTicketCode(ticketCode)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findByPublicCode(String publicCode) {
        return ticketRepository.findWithLinesByPublicCode(publicCode)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findByVerificationCode(String verificationCode) {
        return ticketRepository.findWithLinesByVerificationCode(verificationCode)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findByOfflineSubmissionId(OfflineSubmissionId submissionId) {
        return ticketRepository.findWithLinesByOfflineSubmissionId(submissionId.value())
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findByDrawId(DrawId drawId) {
        var entities = ticketRepository.findWithLinesByDrawId(drawId.value());
        if (!entities.isEmpty()) {
            var ids = entities.stream().map(ticket -> ticket.getId()).toList();
            ticketRepository.findWithChargesByIdIn(ids);
        }

        return entities
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOfflineSubmissionId(OfflineSubmissionId submissionId) {
        return ticketRepository.existsByOfflineSubmissionId(submissionId.value());
    }
}
