package com.tchalanet.server.catalog.settings.query;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.catalog.settings.AppSettingsResolver;
import com.tchalanet.server.catalog.settings.dto.ResolvedSettingDto;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ResolveAppSettingsQueryHandler
    implements QueryHandler<ResolveAppSettingsQuery, List<ResolvedSettingDto>> {

  private final AppSettingsResolver resolver;

  @Override
  public List<ResolvedSettingDto> handle(ResolveAppSettingsQuery query) {
    return resolver.resolve(query);
  }
}
