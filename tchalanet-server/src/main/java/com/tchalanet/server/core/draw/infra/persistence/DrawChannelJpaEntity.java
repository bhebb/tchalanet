package com.tchalanet.server.core.draw.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "draw_channel")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class DrawChannelJpaEntity extends BaseTenantEntity {

    @Column(name = "tenant_game_id", nullable = false)
    private UUID tenantGameId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "draw_time", nullable = false)
    private LocalTime drawTime;

    @Column(name = "cutoff_sec", nullable = false)
    private Integer cutoffSec;

    @Column(name = "days_of_week", nullable = false)
    private String daysOfWeek;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "external_provider")
    private String externalProvider;

    @Column(name = "external_game_key")
    private String externalGameKey;

    @Column(name = "external_channel_code")
    private String externalChannelCode;
}
