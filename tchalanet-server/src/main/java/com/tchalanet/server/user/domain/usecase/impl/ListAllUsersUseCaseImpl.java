package com.tchalanet.server.user.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.user.domain.model.AppUser;
import com.tchalanet.server.user.domain.ports.AppUserRepository;
import com.tchalanet.server.user.domain.usecase.ListAllUsersUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListAllUsersUseCaseImpl implements ListAllUsersUseCase {

  private final AppUserRepository repo;

  @Override
  public List<AppUser> listAll() {
    return repo.findAll();
  }
}

