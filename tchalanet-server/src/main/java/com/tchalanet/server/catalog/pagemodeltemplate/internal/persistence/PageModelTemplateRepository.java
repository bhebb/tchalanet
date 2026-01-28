package com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageModelTemplateRepository
    extends JpaRepository<PageModelTemplateEntity, UUID>, JpaSpecificationExecutor<PageModelTemplateEntity> {

    Optional<PageModelTemplateEntity> findFirstByLogicalId(String logicalId);

    // list all non-deleted ordered by logical id
    List<PageModelTemplateEntity> findAllByDeletedAtIsNullOrderByLogicalIdAsc();

    // find first by logical id among non-deleted rows
    Optional<PageModelTemplateEntity> findFirstByLogicalIdAndDeletedAtIsNull(String logicalId);

    Page<PageModelTemplateEntity> findAllByOrderByUpdatedAtDesc(Pageable pageable);

}
