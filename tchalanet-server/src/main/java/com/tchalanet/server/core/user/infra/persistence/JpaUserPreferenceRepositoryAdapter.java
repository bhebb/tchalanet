package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.core.user.domain.model.UserId;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import com.tchalanet.server.core.user.domain.ports.UserPreferenceRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adapter JPA pour le port UserPreferenceRepository du domaine user. */
@Component
@RequiredArgsConstructor
public class JpaUserPreferenceRepositoryAdapter implements UserPreferenceRepository {

  private final JpaUserPreferenceRepository jpaRepo;
  private final UserPreferenceEntityMapper mapper = new UserPreferenceEntityMapper();

  @Override
  public Optional<UserPreference> findByUserId(UserId userId) {
    return jpaRepo.findById(userId.value()).map(mapper::toDomain);
  }

  @Override
  public Optional<UserPreference> findById(UUID userId) {
    return jpaRepo.findById(userId).map(mapper::toDomain);
  }

  @Override
  public UserPreference save(UserPreference preference) {
    var entity = mapper.toEntity(preference);
    var saved = jpaRepo.save(entity);
    return mapper.toDomain(saved);
  }
}
