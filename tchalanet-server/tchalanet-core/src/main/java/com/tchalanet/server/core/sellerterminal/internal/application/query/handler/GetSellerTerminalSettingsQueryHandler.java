package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalSettingsQuery;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalSettingsReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalSettingsQueryHandler
    implements QueryHandler<GetSellerTerminalSettingsQuery, SellerTerminalSettingsView> {

  private final SellerTerminalSettingsReaderPort reader;

  @Override
  public SellerTerminalSettingsView handle(GetSellerTerminalSettingsQuery query) {
    return reader.get(query.tenantId(), query.sellerTerminalId());
  }
}
