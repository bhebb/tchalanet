package com.tchalanet.server.core.theme.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.theme.application.port.out.ThemeReaderPort;
import com.tchalanet.server.core.theme.application.query.model.GetThemeByIdQuery;
import com.tchalanet.server.core.theme.application.query.model.ThemeView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetThemeByIdQueryHandler implements QueryHandler<GetThemeByIdQuery, ThemeView> {

  private final ThemeReaderPort themeReaderPort;

  @Override
  public ThemeView handle(GetThemeByIdQuery query) {
    var theme =
        themeReaderPort
            .findById(query.themeId())
            .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + query.themeId()));

    if (theme.tenantId() != null && !theme.tenantId().equals(query.tenantId())) {
      throw new AccessDeniedException("Forbidden: Theme does not belong to tenant");
    }

    return ThemeView.fromDomain(theme);
  }
}
