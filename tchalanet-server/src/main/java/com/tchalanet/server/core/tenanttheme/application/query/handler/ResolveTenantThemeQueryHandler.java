package com.tchalanet.server.core.tenanttheme.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenanttheme.application.port.out.TenantThemeReaderPort;
import com.tchalanet.server.core.tenanttheme.application.query.model.ResolveTenantThemeQuery;
import com.tchalanet.server.core.tenanttheme.application.query.model.TenantThemeView;
import lombok.RequiredArgsConstructor;

/**
 * Handler for ResolveTenantThemeQuery.
 * Maps to spec requirement T6.
 */
@UseCase
@RequiredArgsConstructor
public class ResolveTenantThemeQueryHandler
    implements QueryHandler<ResolveTenantThemeQuery, TenantThemeView> {

  private final TenantThemeReaderPort readerPort;

  @Override
  public TenantThemeView handle(ResolveTenantThemeQuery query) {
    return readerPort
        .findByTenantId(query.tenantId())
        .map(
            t ->
                new TenantThemeView(
                    t.tenantId(), t.presetCode(), t.metadata(), t.version(), t.updatedAt()))
        .orElse(null);
  }
}
