package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PageRequest;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.mapper.TicketMapper;
import com.tchalanet.server.core.sales.infra.persistence.repository.SpringTicketJpaRepository;
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
  private final TchRequestContextHolder contextHolder;

  @Override
  public Ticket save(Ticket ticket) {
    TicketEntity entity = mapper.toEntity(ticket);
    TicketEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<Ticket> findById(TicketId ticketId) {
    return jpaRepository.findById(ticketId.uuid()).map(mapper::toDomain);
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
          // tenantId is required
          if (filter.tenantId() != null) {
            predicates.add(cb.equal(root.get("tenantId"), filter.tenantId().uuid()));
          }
          if (filter.terminalId() != null) {
            predicates.add(cb.equal(root.get("terminalId"), filter.terminalId().uuid()));
          }
          if (filter.drawId() != null) {
            predicates.add(cb.equal(root.get("drawId"), filter.drawId().uuid()));
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
        org.springframework.data.domain.PageRequest.of(
            pageRequest.page(),
            pageRequest.size(),
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("createdAt"),
                org.springframework.data.domain.Sort.Order.desc("id")));

    Page<TicketEntity> page = jpaRepository.findAll(spec, springPageRequest);

    List<Ticket> tickets =
        page.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());

    return new PagedResult<>(
        tickets, page.getTotalElements(), page.getTotalPages(), page.getNumber());
  }

  @Override
  public int archiveOldTickets(TenantId tenantId, Instant cutoffDate) {
    return jpaRepository.archiveOldTickets(tenantId.uuid(), cutoffDate, Instant.now(clock));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Ticket> findWithLinesById(TicketId ticketId) {
    return jpaRepository
        .findWithLinesByTenantIdAndId(contextHolder.get().tenantUuid(), ticketId.uuid())
        .map(mapper::toDomain);
  }

  @Override
  public List<Ticket> listRecentForCashier(UserId cashierId, int limit) {
    var pageable = org.springframework.data.domain.PageRequest.of(0, limit);
    return jpaRepository
        .findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(cashierId.uuid(), pageable)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public byte[] exportDailySalesCsv(TenantId tenantId, Instant dayStart, Instant dayEnd) {
    List<TicketEntity> tickets =
        jpaRepository.findByTenantIdAndCreatedAtBetween(tenantId.uuid(), dayStart, dayEnd);

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
            ticket.getStatus(),
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
  public List<AgentDailySalesDto> getAgentDailySales(TenantId tenantId, Instant from, Instant to) {
    return jpaRepository.findAgentDailySales(tenantId.uuid(), from, to);
  }
}
