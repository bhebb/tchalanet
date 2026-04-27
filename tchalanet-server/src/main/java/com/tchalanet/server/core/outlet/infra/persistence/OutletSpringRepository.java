package com.tchalanet.server.core.outlet.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutletSpringRepository extends JpaRepository<OutletEntity, UUID> {
    // RLS at DB level handles tenant scoping for reads. Use JpaRepository methods (findAll, findById, etc.).
}
