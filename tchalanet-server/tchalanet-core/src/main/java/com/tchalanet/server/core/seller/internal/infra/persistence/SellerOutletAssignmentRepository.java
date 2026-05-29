package com.tchalanet.server.core.seller.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SellerOutletAssignmentRepository extends JpaRepository<SellerOutletAssignmentJpaEntity, UUID> {

    @Query("SELECT a FROM SellerOutletAssignmentJpaEntity a WHERE a.sellerId = :sellerId AND a.status = 'ACTIVE' AND a.endsAt IS NULL ORDER BY a.startsAt DESC")
    Optional<SellerOutletAssignmentJpaEntity> findActiveAssignment(@Param("sellerId") UUID sellerId);

    @Query("SELECT a FROM SellerOutletAssignmentJpaEntity a WHERE a.sellerId = :sellerId ORDER BY a.startsAt DESC")
    List<SellerOutletAssignmentJpaEntity> findBySellerId(@Param("sellerId") UUID sellerId);

    @Query("""
        SELECT a FROM SellerOutletAssignmentJpaEntity a
        JOIN SellerJpaEntity s ON s.id = a.sellerId
        WHERE s.userId = :userId AND a.outletId = :outletId
          AND a.status = 'ACTIVE' AND a.endsAt IS NULL
        ORDER BY a.startsAt DESC
        """)
    Optional<SellerOutletAssignmentJpaEntity> findActiveForUserAndOutlet(
        @Param("userId") UUID userId, @Param("outletId") UUID outletId);
}
