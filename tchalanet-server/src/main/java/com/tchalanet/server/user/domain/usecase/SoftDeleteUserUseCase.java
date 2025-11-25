package com.tchalanet.server.user.domain.usecase;

import java.util.UUID;

public interface SoftDeleteUserUseCase {
  void softDelete(UUID userId);
}
