package com.tchalanet.server.platform.tenantgame.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.api.model.TenantBetTypeOptionConfig;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateTenantGameBetOptionConfigRequest {
    private TenantId tenantId;
    private String gameCode;
    private List<TenantBetTypeOptionConfig> betTypes;
}
