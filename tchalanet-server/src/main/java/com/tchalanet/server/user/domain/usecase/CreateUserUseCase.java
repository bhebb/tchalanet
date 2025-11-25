package com.tchalanet.server.user.domain.usecase;

import com.tchalanet.server.user.domain.model.AppUser;

public interface CreateUserUseCase {
  AppUser create(AppUser user);
}
