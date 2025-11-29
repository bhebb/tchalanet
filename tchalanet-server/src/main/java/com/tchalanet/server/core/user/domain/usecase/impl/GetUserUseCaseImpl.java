package com.tchalanet.server.core.user.domain.usecase.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.ports.AppUserRepository;
import com.tchalanet.server.core.user.domain.usecase.GetUserUseCase;
import java.util.Optional;
import java.util.UUID;

@UseCase
public class GetUserUseCaseImpl implements GetUserUseCase {

  private final AppUserRepository repo;

  public GetUserUseCaseImpl(AppUserRepository repo) {
    this.repo = repo;
  }

  @Override
  public Optional<AppUser> getById(UUID id) {
    return repo.findById(id);
  }
}
