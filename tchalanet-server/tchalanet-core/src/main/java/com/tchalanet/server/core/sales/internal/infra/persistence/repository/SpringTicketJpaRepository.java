package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.core.sales.api.query.AgentDailySalesDto;
import com.tchalanet.server.core.sales.internal.infra.persistence.TicketJpaEntity;
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
    extends JpaRepository<TicketJpaEntity, UUID>, JpaSpecificationExecutor<TicketJpaEntity> {

    @Modifying
    @Query(
        "UPDATE TicketJpaEntity t SET t.deletedAt = :now WHERE t.createdAt < :cutoffDate AND t.deletedAt IS NULL")
    int archiveOldTickets(
        @Param("cutoffDate") Instant cutoffDate,
        @Param("now") Instant now);

    List<TicketJpaEntity> findByCreatedAtBetween(Instant from, Instant to);

    @EntityGraph(attributePaths = "lines")
    Optional<TicketJpaEntity> findWithLinesById(UUID id);

    List<TicketJpaEntity> findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
        UUID createdBy, Pageable pageable);

    @Query(
        "SELECT new com.tchalanet.server.core.sales.api.query.AgentDailySalesDto(t.createdBy, SUM(t.totalAmount), COUNT(t.id)) "
            + "FROM TicketJpaEntity t "
            + "WHERE t.createdAt BETWEEN :from AND :to AND t.deletedAt IS NULL "
            + "GROUP BY t.createdBy")
    List<AgentDailySalesDto> findAgentDailySales(@Param("from") Instant from, @Param("to") Instant to);

    // Counts for CloseDay stats — filter deleted_at IS NULL to exclude archived tickets
    long countBySessionIdInAndDeletedAtIsNull(List<UUID> sessionIds);

    long countBySessionIdInAndSaleStatusAndDeletedAtIsNull(List<UUID> sessionIds, com.tchalanet.server.core.sales.api.model.TicketSaleStatus saleStatus);

    long countBySessionIdInAndResultStatusAndDeletedAtIsNull(List<UUID> sessionIds, com.tchalanet.server.core.sales.api.model.TicketResultStatus resultStatus);

    long countBySessionIdInAndSettlementStatusAndDeletedAtIsNull(List<UUID> sessionIds, com.tchalanet.server.core.sales.api.model.TicketSettlementStatus settlementStatus);


    @EntityGraph(attributePaths = "lines")
    Optional<TicketJpaEntity> findById(UUID id);

    // find by public code already exists above; keep convenience method
    @EntityGraph(attributePaths = "lines")
    Optional<TicketJpaEntity> findByPublicCodeAndDeletedAtIsNull(String publicCode);

    Page<TicketJpaEntity> findByCreatedAtBetweenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Instant from, Instant to, Pageable pageable);

    long countByDrawIdAndResultStatusAndDeletedAtIsNull(UUID drawId, com.tchalanet.server.core.sales.api.model.TicketResultStatus resultStatus);
}
