package com.tchalanet.server.catalog.billing.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = true)
public interface SubscriptionRestRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {}
