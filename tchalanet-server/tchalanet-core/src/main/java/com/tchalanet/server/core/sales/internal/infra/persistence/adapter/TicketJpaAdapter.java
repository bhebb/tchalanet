package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.infra.persistence.mapper.TicketJpaMapper;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketChargeJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketLineJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TicketJpaAdapter implements TicketReaderPort, TicketWriterPort {

    private final TicketJpaRepository ticketRepository;
    private final TicketLineJpaRepository lineRepository;
    private final TicketChargeJpaRepository chargeRepository;
    private final TicketJpaMapper mapper;

    @Override
    @Transactional
    public Ticket save(Ticket ticket) {
        var entity = mapper.toEntity(ticket);
        // The mapper rebuilds the entity (and its lines/charges) from the domain aggregate,
        // which loses Hibernate's managed state including @Version. For the update path,
        // transplant the current versions from the DB so JPA's optimistic-locking merge passes.
        var ticketId = entity.getId();
        ticketRepository.findVersionById(ticketId).ifPresent(entity::setVersion);

        Map<UUID, Long> lineVersions = lineRepository.findVersionsByTicketId(ticketId).stream()
            .collect(Collectors.toMap(
                TicketLineJpaRepository.TicketLineVersionView::getId,
                TicketLineJpaRepository.TicketLineVersionView::getVersion));
        for (var line : entity.getLines()) {
            var v = lineVersions.get(line.getId());
            if (v != null) {
                line.setVersion(v);
            }
        }

        Map<UUID, Long> chargeVersions = chargeRepository.findVersionsByTicketId(ticketId).stream()
            .collect(Collectors.toMap(
                TicketChargeJpaRepository.TicketChargeVersionView::getId,
                TicketChargeJpaRepository.TicketChargeVersionView::getVersion));
        for (var charge : entity.getCharges()) {
            var v = chargeVersions.get(charge.getId());
            if (v != null) {
                charge.setVersion(v);
            }
        }

        var saved = ticketRepository.save(entity);
        return mapper.toDomain(saved);
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
