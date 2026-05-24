package com.tchalanet.server.core.agent.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agent")
@Audited
@Getter
@Setter
public class AgentJpaEntity extends BaseTenantEntity {

    @Column(name = "parent_agent_id")
    private UUID parentAgentId;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "primary_zone_id", nullable = false)
    private UUID primaryZoneId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(nullable = false)
    private int depth;

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AgentZoneMandateJpaEntity> mandates = new ArrayList<>();
}
