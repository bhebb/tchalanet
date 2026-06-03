package com.tchalanet.server.platform.tenantgame.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class UpdateTenantGameSettingsRequest {
    private TenantId tenantId;
    private String gameCode;
    private String displayName;
    private Integer displayOrder;
    private Boolean visibleInPos;
    private BigDecimal minStake;
    private BigDecimal maxStake;
    private Boolean availabilityEnabled;
    private String availabilityDays;
    private String startLocalTime;
    private String endLocalTime;
}
