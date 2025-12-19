package com.tchalanet.server.core.theme.application.port.out;

import com.tchalanet.server.core.theme.domain.model.Theme;

public interface ThemeWriterPort {

  Theme save(Theme theme);
}

