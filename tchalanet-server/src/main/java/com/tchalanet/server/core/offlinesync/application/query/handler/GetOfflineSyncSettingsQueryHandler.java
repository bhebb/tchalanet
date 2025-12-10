package com.tchalanet.server.core.offlinesync.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.query.model.GetOfflineSyncSettingsQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetOfflineSyncSettingsQueryHandler implements QueryHandler<GetOfflineSyncSettingsQuery, Map<String,Object>> {

  @Override
  public Map<String,Object> handle(GetOfflineSyncSettingsQuery query) {
    // TODO: return settings
    throw new UnsupportedOperationException("GetOfflineSyncSettingsQueryHandler not implemented yet");
  }
}

