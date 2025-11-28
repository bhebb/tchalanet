package com.tchalanet.server.user.domain.usecase.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.user.domain.model.AppUser;
import com.tchalanet.server.user.domain.ports.AppUserRepository;
import com.tchalanet.server.user.domain.usecase.UpdateUserUseCase;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class UpdateUserUseCaseImpl implements UpdateUserUseCase {

  private final AppUserRepository repo;

  public UpdateUserUseCaseImpl(AppUserRepository repo) {
    this.repo = repo;
  }

  @Transactional
  @Override
  public AppUser update(AppUser user) {
    // basic: delegate to repo.save
    return repo.save(user);
  }
}
