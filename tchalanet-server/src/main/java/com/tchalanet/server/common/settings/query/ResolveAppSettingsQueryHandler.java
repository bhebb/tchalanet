package com.tchalanet.server.common.settings.query;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.settings.AppSettingsResolver;
import com.tchalanet.server.common.settings.dto.ResolvedSettingDto;
import com.tchalanet.server.common.stereotype.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
