package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.ports.in.ListTicketsQuery.PageRequest;
import com.tchalanet.server.core.sales.domain.ports.in.ListTicketsQuery.PagedResult;
import com.tchalanet.server.core.sales.domain.ports.in.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.domain.ports.out.ClockPort;
import com.tchalanet.server.core.sales.domain.ports.out.TicketRepositoryPort;
import com.tchalanet.server.core.sales.infra.persistence.entity.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.mapper.TicketMapper;
import com.tchalanet.server.core.sales.infra.persistence.repository.SpringTicketJpaRepository;
import jakarta.persistence.criteria.Predicate;
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

@Component
@RequiredArgsConstructor
public class JpaTicketRepositoryAdapter implements TicketRepositoryPort {

  private final SpringTicketJpaRepository jpaRepository;
  private final TicketMapper mapper;
  private final ClockPort clock;

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
        org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());

    Page<TicketEntity> page = jpaRepository.findAll(spec, springPageRequest);

    List<Ticket> tickets =
        page.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());

    return new PagedResult<>(
        tickets, page.getTotalElements(), page.getTotalPages(), page.getNumber());
  }

  @Override
  public int archiveOldTickets(UUID tenantId, Instant cutoffDate) {
    return jpaRepository.archiveOldTickets(tenantId, cutoffDate, clock.now());
  }
}
