package com.tchalanet.server.core.drawresult.infra.persistence.repo;

import com.tchalanet.server.core.drawresult.infra.persistence.DrawResultJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrawResultJpaRepository extends JpaRepository<DrawResultJpaEntity, UUID> {}
