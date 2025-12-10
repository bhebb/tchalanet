package com.tchalanet.server.core.theme.application.query.handler;

import com.tchalanet.server.core.theme.infra.persistence.ThemeJpaEntity;
import java.util.List;
import java.util.UUID;

public interface ListThemesUseCase {
  List<ThemeJpaEntity> list(UUID tenantId);
}
