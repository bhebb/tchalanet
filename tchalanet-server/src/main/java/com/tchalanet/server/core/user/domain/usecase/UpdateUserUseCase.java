package com.tchalanet.server.core.user.domain.usecase;

import com.tchalanet.server.core.user.domain.model.AppUser;

public interface UpdateUserUseCase {
  AppUser update(AppUser user);
}
