package com.tchalanet.server.core.billing.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "admin-subscriptions", collectionResourceRel = "subscriptions")
public interface SubscriptionRestRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {}
