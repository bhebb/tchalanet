package com.tchalanet.server.catalog.theme.application.port.out;

import com.tchalanet.server.catalog.theme.domain.model.Theme;

public interface ThemeWriterPort {

  Theme save(Theme theme);
}
