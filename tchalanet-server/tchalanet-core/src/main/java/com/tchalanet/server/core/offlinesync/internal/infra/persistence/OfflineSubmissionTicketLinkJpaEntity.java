package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Audited
@Table(name = "offline_submission_ticket_link")
public class OfflineSubmissionTicketLinkJpaEntity extends BaseTenantEntity {

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "link_type", nullable = false)
    private String linkType;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;
}
