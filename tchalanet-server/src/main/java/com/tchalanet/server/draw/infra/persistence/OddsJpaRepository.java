package com.tchalanet.server.draw.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OddsJpaRepository extends JpaRepository<OddsJpaEntity, UUID> {}
