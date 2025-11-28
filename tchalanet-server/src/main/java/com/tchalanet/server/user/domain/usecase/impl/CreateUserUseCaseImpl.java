package com.tchalanet.server.user.domain.usecase.impl;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.user.domain.model.AppUser;
import com.tchalanet.server.user.domain.ports.AppUserRepository;
import com.tchalanet.server.user.domain.usecase.CreateUserUseCase;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class CreateUserUseCaseImpl implements CreateUserUseCase {

  private final AppUserRepository repo;

  public CreateUserUseCaseImpl(AppUserRepository repo) {
    this.repo = repo;
  }

  @Transactional
  @Override
  public AppUser create(AppUser user) {
    // if id is null -> create one (the DB might expect keycloak sub; allow null and DB will gen)
    UUID id = user.id();
    if (id == null) id = UUID.randomUUID();
    AppUser toSave =
        new AppUser(
            id,
            user.tenantId(),
            user.username(),
            user.email(),
            user.displayName(),
            user.locale(),
            user.lastLoginAt());
    return repo.save(toSave);
  }
}
