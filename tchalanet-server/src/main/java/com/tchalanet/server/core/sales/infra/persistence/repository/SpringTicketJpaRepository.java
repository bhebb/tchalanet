package com.tchalanet.server.core.sales.infra.persistence.repository;

import com.tchalanet.server.core.sales.application.query.model.AgentDailySalesDto;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringTicketJpaRepository
    extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {

    @EntityGraph(attributePaths = "lines")
    Optional<TicketEntity> findByPublicCode(String publicCode);

    @Modifying
    @Query(
        "UPDATE TicketEntity t SET t.deletedAt = :now WHERE t.tenantId = :tenantId AND t.createdAt < :cutoffDate AND t.deletedAt IS NULL")
    int archiveOldTickets(
        @Param("tenantId") UUID tenantId,
        @Param("cutoffDate") Instant cutoffDate,
        @Param("now") Instant now);

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


    @EntityGraph(attributePaths = "lines")
    Optional<TicketEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @EntityGraph(attributePaths = "lines")
    Optional<TicketEntity> findByTenantIdAndPublicCode(UUID tenantId, String publicCode);
    Page<TicketEntity> findByTenantIdAndCreatedAtBetweenAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID tenantId, Instant from, Instant to, Pageable pageable);

    long countByTenantIdAndDrawIdAndStatusAndDeletedAtIsNull(UUID tenantId, UUID drawId, TicketStatus status);
}
