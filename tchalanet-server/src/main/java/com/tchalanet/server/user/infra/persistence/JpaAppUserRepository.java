package com.tchalanet.server.user.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAppUserRepository extends JpaRepository<AppUserJpaEntity, UUID> {}
