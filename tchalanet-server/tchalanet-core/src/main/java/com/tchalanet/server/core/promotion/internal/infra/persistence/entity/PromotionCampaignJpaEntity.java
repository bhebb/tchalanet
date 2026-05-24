package com.tchalanet.server.core.promotion.internal.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.promotion.api.model.PromotionCampaignStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.Instant;

@Entity
@Audited
@Table(name = "promotion_campaign", uniqueConstraints = @UniqueConstraint(name = "uq_promotion_campaign_tenant_code", columnNames = {"tenant_id", "code"}))
@Getter
@Setter
public class PromotionCampaignJpaEntity extends BaseTenantEntity {
    @Column(nullable = false, length = 96)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private PromotionCampaignStatus status;

    @Column(nullable = false)
    private int priority;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "config_version", nullable = false, length = 48)
    private String configVersion;
}
