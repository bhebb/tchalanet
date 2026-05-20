package com.tchalanet.server.platform.tenantgame.internal.web.mapper;

import com.tchalanet.server.platform.tenantgame.internal.service.TenantGame;
import com.tchalanet.server.platform.tenantgame.internal.web.TenantGameView;
import org.springframework.stereotype.Component;

@Component
public class TenantGameWebMapper {

  public TenantGameView toView(TenantGame domain) {
    return TenantGameView.builder()
        .gameCode(domain.code())
        .enabled(Boolean.TRUE.equals(domain.enabled()))
        .displayName(domain.displayName())
        .minStake(domain.minStake())
        .maxStake(domain.maxStake())
        .flags(domain.flags())
        .build();
  }
}

