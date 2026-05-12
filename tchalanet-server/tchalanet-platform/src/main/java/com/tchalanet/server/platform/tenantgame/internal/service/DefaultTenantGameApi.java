package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameCommand;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameCommandResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameCommand;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameCommandResult;
import com.tchalanet.server.platform.tenantgame.api.model.ResolveTenantGamesQuery;
import com.tchalanet.server.platform.tenantgame.api.model.UpdateTenantGamePolicyCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultTenantGameApi implements TenantGameApi {

  private final TenantGameService service;

  @Override
  public EnableTenantGameCommandResult enableTenantGame(EnableTenantGameCommand request) {
    return service.enableTenantGame(request);
  }

  @Override
  public DisableTenantGameCommandResult disableTenantGame(DisableTenantGameCommand request) {
    return service.disableTenantGame(request);
  }

  @Override
  public List<Object> resolveTenantGames(ResolveTenantGamesQuery request) {
    return List.copyOf(service.resolveTenantGames(request));
  }

  @Override
  public void updateTenantGamePolicy(UpdateTenantGamePolicyCommand request) {
    service.updateTenantGamePolicy(request);
  }
}
