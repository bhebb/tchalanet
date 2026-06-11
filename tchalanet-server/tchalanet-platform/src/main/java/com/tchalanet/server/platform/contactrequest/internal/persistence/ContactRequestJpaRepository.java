package com.tchalanet.server.platform.contactrequest.internal.persistence;

import com.tchalanet.server.platform.contactrequest.api.ContactRequestIntent;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRequestJpaRepository extends JpaRepository<ContactRequestJpaEntity, UUID> {

    @Query("""
        SELECT c FROM ContactRequestJpaEntity c
        WHERE c.deletedAt IS NULL
          AND (:status IS NULL OR c.status = :status)
          AND (:intent IS NULL OR c.intent = :intent)
        """)
    Page<ContactRequestJpaEntity> search(
        @Param("status") ContactRequestStatus status,
        @Param("intent") ContactRequestIntent intent,
        Pageable pageable);
}
