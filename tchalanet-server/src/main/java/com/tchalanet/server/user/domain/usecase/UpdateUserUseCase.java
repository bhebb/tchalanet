package com.tchalanet.server.user.domain.usecase;

import com.tchalanet.server.user.domain.model.AppUser;

public interface UpdateUserUseCase {
  AppUser update(AppUser user);
}
