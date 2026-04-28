package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import com.tchalanet.server.core.sales.application.port.out.TicketPrintView;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.application.print.TicketPrintViewMapper;
import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.mapper.TicketMapper;
import com.tchalanet.server.core.sales.infra.persistence.repository.SpringTicketJpaRepository;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaTicketRepositoryAdapter implements TicketWritterPort, TicketReaderPort {

  private final SpringTicketJpaRepository jpaRepository;
  private final TicketMapper mapper;
  private final Clock clock;
  private final TchContextResolver contextResolver;
  private final DrawLookupPort drawReader;
  private final TicketPrintViewMapper ticketPrintViewMapper;
  private final OutletReaderPort outletReaderPort;
  private final SalesSessionReaderPort posSessionReaderPort;

  @Override
  public Ticket save(Ticket ticket) {
    TicketEntity entity = mapper.toEntity(ticket);
    TicketEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<Ticket> findById(TicketId ticketId) {
    return jpaRepository.findById(ticketId.value()).map(mapper::toDomain);
  }

  @Override
  public Optional<Ticket> findByPublicCode(String publicCode) {
    return jpaRepository.findByPublicCode(publicCode).map(mapper::toDomain);
  }

  @Override
  public TchPage<Ticket> search(TicketFilter filter, Pageable pageRequest) {
    Specification<TicketEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          // tenantId is required — removed: RLS ensures tenant isolation
          if (filter.terminalId() != null) {
            predicates.add(cb.equal(root.get("terminalId"), filter.terminalId().value()));
          }
          if (filter.drawId() != null) {
            predicates.add(cb.equal(root.get("drawId"), filter.drawId().value()));
          }
          if (filter.status() != null) {
            // status in filter corresponds to resultStatus
            predicates.add(cb.equal(root.get("resultStatus"), filter.status()));
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
        org.springframework.data.domain.PageRequest.of(
            pageRequest.getPageNumber(),
            pageRequest.getPageSize(),
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("createdAt"),
                org.springframework.data.domain.Sort.Order.desc("id")));

    Page<TicketEntity> page = jpaRepository.findAll(spec, springPageRequest);

    List<Ticket> tickets =
        page.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());

    return TchPage.of(
        tickets,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast(),
        page.hasNext(),
        page.hasPrevious());
  }

  @Override
  public int archiveOldTickets(Instant cutoffDate) {
    return jpaRepository.archiveOldTickets(cutoffDate, Instant.now(clock));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Ticket> findWithLinesById(TicketId ticketId) {
    var holder = contextResolver.currentOrNull();
    return jpaRepository
        .findWithLinesById(ticketId.value())
        .map(mapper::toDomain);
  }

  @Override
  public List<Ticket> listRecentForCashier(UserId cashierId, int limit) {
    var pageable = org.springframework.data.domain.PageRequest.of(0, limit);
    return jpaRepository
        .findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(cashierId.value(), pageable)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public byte[] exportDailySalesCsv(Instant dayStart, Instant dayEnd) {
    List<TicketEntity> tickets =
        jpaRepository.findByCreatedAtBetween(dayStart, dayEnd);

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

      // Write header
      writer.println(
          "createdAt,ticketCode,publicCode,status,totalAmount,terminalId,drawId,sessionId,createdBy");

      // Write data
      for (TicketEntity ticket : tickets) {
        writer.printf(
            "%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
            ticket.getCreatedAt(),
            ticket.getTicketCode(),
            ticket.getPublicCode() != null ? ticket.getPublicCode() : "",
            ticket.getResultStatus(),
            ticket.getTotalAmount(),
            ticket.getTerminalId(),
            ticket.getDrawId(),
            ticket.getSessionId() != null ? ticket.getSessionId() : "",
            ticket.getCreatedBy() != null ? ticket.getCreatedBy() : "");
      }

      writer.flush();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate CSV", e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public TicketPrintView getTicketPrintView(TicketId ticketId) {
    // 1. Load Ticket with lines
    var ticket =
        jpaRepository
            .findWithLinesById(ticketId.value())
            .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

    // 2. Load Draw
    var draw = drawReader.findById(DrawId.of(ticket.getDrawId())).orElse(null);

    // 3. Load Channel (via Draw)
    var channel = (draw != null) ? draw.drawChannel() : null;
    Outlet outlet = null;
    var session = posSessionReaderPort.findById(SessionId.of(ticket.getSessionId())).orElse(null);
    if (session != null) {
      outlet = outletReaderPort.findById(session.outletId()).orElse(null);
    }
    // TODO: get locale from context or request
    return ticketPrintViewMapper.map(mapper.toDomain(ticket), outlet, draw, channel, Locale.FRENCH);
  }

  @Override
  public List<AgentDailySalesDto> getAgentDailySales(Instant from, Instant to) {
    return jpaRepository.findAgentDailySales(from, to);
  }
}
