package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.ResolveTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGamePolicyRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultTenantGameApi implements TenantGameApi {

  private final TenantGameService service;

  @Override
  public EnableTenantGameResult enableTenantGame(EnableTenantGameRequest request) {
    return service.enableTenantGame(request);
  }

  @Override
  public DisableTenantGameResult disableTenantGame(DisableTenantGameRequest request) {
    return service.disableTenantGame(request);
  }

  @Override
  public List<Object> resolveTenantGames(ResolveTenantGamesRequest request) {
    return List.copyOf(service.resolveTenantGames(request));
  }

  @Override
  public void updateTenantGamePolicy(UpdateTenantGamePolicyRequest request) {
    service.updateTenantGamePolicy(request);
  }
}
