package com.tchalanet.server.user.domain.usecase;

import com.tchalanet.server.user.domain.model.AppUser;
import java.util.Optional;
import java.util.UUID;

public interface GetUserUseCase {
  Optional<AppUser> getById(UUID id);
}
