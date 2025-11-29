package com.tchalanet.server.core.user.domain.usecase;

import com.tchalanet.server.core.user.domain.model.AppUser;

public interface CreateUserUseCase {
  AppUser create(AppUser user);
}
