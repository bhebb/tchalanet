package com.tchalanet.server.pos.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TerminalJpaRepository extends JpaRepository<TerminalJpaEntity, UUID> {}
