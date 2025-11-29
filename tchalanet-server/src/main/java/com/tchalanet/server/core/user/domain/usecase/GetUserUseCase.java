package com.tchalanet.server.core.user.domain.usecase;

import com.tchalanet.server.core.user.domain.model.AppUser;
import java.util.Optional;
import java.util.UUID;

public interface GetUserUseCase {
  Optional<AppUser> getById(UUID id);
}
