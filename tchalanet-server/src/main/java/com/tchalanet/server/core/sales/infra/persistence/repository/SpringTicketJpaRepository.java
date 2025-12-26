package com.tchalanet.server.core.sales.infra.persistence.repository;

import com.tchalanet.server.common.types.enums.TicketStatus;
import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringTicketJpaRepository
    extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {
  Optional<TicketEntity> findByPublicCode(String publicCode);

  @Modifying
  @Query(
      "UPDATE TicketEntity t SET t.deletedAt = :now WHERE t.tenantId = :tenantId AND t.createdAt < :cutoffDate AND t.deletedAt IS NULL")
  int archiveOldTickets(
      @Param("tenantId") UUID tenantId,
      @Param("cutoffDate") Instant cutoffDate,
      @Param("now") Instant now);

  List<TicketEntity> findByDrawId(UUID drawId); // New method for Stats domain

  Page<TicketEntity> findByTenantIdAndSessionIdInOrderByCreatedAtDesc(
      UUID tenantId, List<UUID> sessionIds, Pageable pageable);

  Optional<TicketEntity> findByTenantIdAndId(UUID tenantId, UUID id);

  Optional<TicketEntity> findByTenantIdAndPublicCode(UUID tenantId, String publicCode);

  Optional<TicketEntity> findByTenantIdAndTicketCode(UUID tenantId, String ticketCode);

  List<TicketEntity> findTop50ByTenantIdAndTerminalIdOrderByCreatedAtDesc(
      UUID tenantId, UUID terminalId);

  List<TicketEntity> findByTenantIdAndStatusAndCreatedAtBetween(
      UUID tenantId, TicketStatus status, Instant from, Instant to);

  List<TicketEntity> findByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);

  @EntityGraph(attributePaths = "lines")
  Optional<TicketEntity> findWithLinesByTenantIdAndId(UUID tenantId, UUID id);

  List<TicketEntity> findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
      UUID createdBy, Pageable pageable);

  @Query(
      "SELECT new com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto(t.createdBy, SUM(t.totalAmount), COUNT(t.id)) "
          + "FROM TicketEntity t "
          + "WHERE t.tenantId = :tenantId AND t.createdAt BETWEEN :from AND :to AND t.deletedAt IS NULL "
          + "GROUP BY t.createdBy")
  List<AgentDailySalesDto> findAgentDailySales(
      @Param("tenantId") UUID tenantId, @Param("from") Instant from, @Param("to") Instant to);

  // Counts for CloseDay stats
  long countByTenantIdAndSessionIdInAndCreatedAtBetween(
      UUID tenantId, List<UUID> sessionIds, Instant from, Instant to);

  long countByTenantIdAndSessionIdInAndCreatedAtBetweenAndStatus(
      UUID tenantId, List<UUID> sessionIds, Instant from, Instant to, TicketStatus status);
}
