package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.mapper.TicketMapper;
import com.tchalanet.server.core.sales.infra.persistence.repository.SpringTicketJpaRepository;
import com.tchalanet.server.core.sales.infra.persistence.repository.TicketSpecifications;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaTicketRepositoryAdapter implements TicketWriterPort, TicketReaderPort {

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
  public Optional<Ticket> findById(TicketId ticketId) {
    return jpaRepository.findById(ticketId.value()).map(mapper::toDomain);
  }

  @Override
  public Optional<Ticket> findByPublicCode(String publicCode) {
    return jpaRepository.findByPublicCodeAndDeletedAtIsNull(publicCode).map(mapper::toDomain);
  }

  @Override
  public TchPage<Ticket> search(TicketFilter filter, Pageable pageRequest) {
    Page<TicketEntity> page = jpaRepository.findAll(TicketSpecifications.fromFilter(filter), pageRequest);

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
          "createdAt,ticketCode,publicCode,status,totalAmount,id,drawId,sessionId,createdBy");

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
  public List<AgentDailySalesDto> getAgentDailySales(Instant from, Instant to) {
    return jpaRepository.findAgentDailySales(from, to);
  }
}
