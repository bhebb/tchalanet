package com.tchalanet.server.core.payout.infra.rest.payout;

import com.tchalanet.server.core.payout.infra.persistence.PayoutJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = true)
public interface PayoutRestRepository extends JpaRepository<PayoutJpaEntity, UUID> {}
