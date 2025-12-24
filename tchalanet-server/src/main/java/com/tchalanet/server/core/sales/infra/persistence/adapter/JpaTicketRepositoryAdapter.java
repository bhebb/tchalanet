package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PageRequest;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.mapper.TicketMapper;
import com.tchalanet.server.core.sales.infra.persistence.repository.SpringTicketJpaRepository;
import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import jakarta.persistence.criteria.Predicate;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaTicketRepositoryAdapter implements TicketWritterPort, TicketReaderPort {

    private final SpringTicketJpaRepository jpaRepository;
    private final TicketMapper mapper;
    private final Clock clock;

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity = mapper.toEntity(ticket);
        TicketEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Ticket> findById(UUID ticketId) {
        return jpaRepository.findById(ticketId).map(mapper::toDomain);
    }

    @Override
    public Optional<Ticket> findByPublicCode(String publicCode) {
        return jpaRepository.findByPublicCode(publicCode).map(mapper::toDomain);
    }

    @Override
    public PagedResult<Ticket> search(TicketFilter filter, PageRequest pageRequest) {
        Specification<TicketEntity> spec =
            (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("tenantId"), filter.tenantId()));
                if (filter.terminalId() != null) {
                    predicates.add(cb.equal(root.get("terminalId"), filter.terminalId()));
                }
                if (filter.drawId() != null) {
                    predicates.add(cb.equal(root.get("drawId"), filter.drawId()));
                }
                if (filter.status() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.status()));
                }
                if (filter.from() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
                }
                if (filter.to() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.to()));
                }
                // Ensure we don't fetch archived tickets in normal searches
                predicates.add(cb.isNull(root.get("deletedAt")));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

        org.springframework.data.domain.PageRequest springPageRequest =
            org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(),
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("createdAt"),
                    org.springframework.data.domain.Sort.Order.desc("id")
                ));

        Page<TicketEntity> page = jpaRepository.findAll(spec, springPageRequest);

        List<Ticket> tickets =
            page.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());

        return new PagedResult<>(
            tickets, page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }

    @Override
    public int archiveOldTickets(UUID tenantId, Instant cutoffDate) {
        return jpaRepository.archiveOldTickets(tenantId, cutoffDate, Instant.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findWithLinesById(UUID tenantId, UUID ticketId) {
        return jpaRepository.findWithLinesByTenantIdAndId(tenantId, ticketId).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> listRecentForCashier(UUID cashierId, int limit) {
        var pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return jpaRepository.findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(cashierId, pageable)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public byte[] exportDailySalesCsv(UUID tenantId, Instant dayStart, Instant dayEnd) {
        List<TicketEntity> tickets = jpaRepository.findByTenantIdAndCreatedAtBetween(
            tenantId, dayStart, dayEnd);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            // Write header
            writer.println("createdAt,ticketCode,publicCode,status,totalAmount,terminalId,drawId,sessionId,createdBy");

            // Write data
            for (TicketEntity ticket : tickets) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    ticket.getCreatedAt(),
                    ticket.getTicketCode(),
                    ticket.getPublicCode() != null ? ticket.getPublicCode() : "",
                    ticket.getStatus(),
                    ticket.getTotalAmount(),
                    ticket.getTerminalId(),
                    ticket.getDrawId(),
                    ticket.getSessionId() != null ? ticket.getSessionId() : "",
                    ticket.getCreatedBy() != null ? ticket.getCreatedBy() : ""
                );
            }

            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    @Override
    public List<AgentDailySalesDto> getAgentDailySales(UUID tenantId, Instant from, Instant to) {
        return jpaRepository.findAgentDailySales(tenantId, from, to);
    }
}
