package com.tchalanet.server.core.resultslot.internal.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(path = "result-slots", collectionResourceRel = "resultSlots")
public interface ResultSlotRestRepository
    extends PagingAndSortingRepository<ResultSlotJpaEntity, UUID> {

  @RestResource(path = "by-slot-key", rel = "by-slot-key")
  Optional<ResultSlotJpaEntity> findFirstBySlotKeyIgnoreCase(String key);

  @RestResource(path = "active", rel = "active")
  Page<ResultSlotJpaEntity> findByActiveTrue(Pageable pageable);
}
