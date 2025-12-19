package com.tchalanet.server.core.theme.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.theme.application.port.out.ThemeReaderPort;
import com.tchalanet.server.core.theme.application.query.model.ListThemesQuery;
import com.tchalanet.server.core.theme.application.query.model.ThemeView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListThemesQueryHandler
    implements QueryHandler<ListThemesQuery, List<ThemeView>> {

  private final ThemeReaderPort themeReaderPort;

  @Override
  public List<ThemeView> handle(ListThemesQuery query) {
    var themes = themeReaderPort.listByTenantAndStatus(query.tenantId(), query.status());
    return themes.stream().map(ThemeView::fromDomain).toList();
  }
}

