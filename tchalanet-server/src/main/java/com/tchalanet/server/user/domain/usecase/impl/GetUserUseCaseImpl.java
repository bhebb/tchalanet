package com.tchalanet.server.user.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.user.domain.model.AppUser;
import com.tchalanet.server.user.domain.ports.AppUserRepository;
import com.tchalanet.server.user.domain.usecase.GetUserUseCase;
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
