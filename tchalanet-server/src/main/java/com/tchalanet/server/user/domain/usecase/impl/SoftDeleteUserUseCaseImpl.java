package com.tchalanet.server.user.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.user.domain.ports.AppUserRepository;
import com.tchalanet.server.user.domain.usecase.SoftDeleteUserUseCase;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class SoftDeleteUserUseCaseImpl implements SoftDeleteUserUseCase {

  private final AppUserRepository repo;

  public SoftDeleteUserUseCaseImpl(AppUserRepository repo) {
    this.repo = repo;
  }

  @Transactional
  @Override
  public void softDelete(UUID userId) {
    repo.softDelete(userId, Instant.now());
  }
}
