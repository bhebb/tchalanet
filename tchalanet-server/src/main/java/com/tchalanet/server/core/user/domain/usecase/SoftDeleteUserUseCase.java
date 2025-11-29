package com.tchalanet.server.core.user.domain.usecase;

import java.util.UUID;

public interface SoftDeleteUserUseCase {
  void softDelete(UUID userId);
}
