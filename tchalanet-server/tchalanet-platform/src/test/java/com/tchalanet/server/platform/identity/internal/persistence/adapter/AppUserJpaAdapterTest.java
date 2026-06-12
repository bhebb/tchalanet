package com.tchalanet.server.platform.identity.internal.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserExternalIdentityJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserExternalIdentityJpaRepository;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AppUserJpaAdapterTest {

  private final AppUserJpaRepository users = Mockito.mock(AppUserJpaRepository.class);
  private final AppUserExternalIdentityJpaRepository externalIdentities =
      Mockito.mock(AppUserExternalIdentityJpaRepository.class);
  private final AppUserJpaAdapter adapter =
      new AppUserJpaAdapter(users, externalIdentities, Mockito.mock(EntityManager.class));

  @Test
  void resolvesKeycloakSubjectThroughExternalIdentityMapping() {
    var appUserId = UUID.randomUUID();
    var keycloakId = UUID.randomUUID();
    var identity = new AppUserExternalIdentityJpaEntity();
    identity.setAppUserId(appUserId);
    identity.setProvider(IdentityProviderType.KEYCLOAK);
    identity.setIssuer("legacy:keycloak");
    identity.setExternalSubject(keycloakId.toString());
    var entity = activeUser(appUserId);
    when(externalIdentities.findFirstByProviderAndExternalSubject(
            IdentityProviderType.KEYCLOAK, keycloakId.toString()))
        .thenReturn(Optional.of(identity));
    when(externalIdentities.findFirstByAppUserIdAndProvider(
            appUserId, IdentityProviderType.KEYCLOAK))
        .thenReturn(Optional.of(identity));
    when(users.findById(appUserId)).thenReturn(Optional.of(entity));

    var resolved = adapter.findByKeycloakSub(KeycloakUserSub.of(keycloakId));

    assertThat(resolved).isPresent();
    assertThat(resolved.orElseThrow().id().value()).isEqualTo(appUserId);
    assertThat(resolved.orElseThrow().keycloakSub().value()).isEqualTo(keycloakId);
  }

  @Test
  void persistsKeycloakSubjectAsExternalIdentityForNewAppUser() {
    var appUserId = UUID.randomUUID();
    var keycloakId = UUID.randomUUID();
    when(users.save(any(AppUserJpaEntity.class)))
        .thenAnswer(
            invocation -> {
              var entity = invocation.getArgument(0, AppUserJpaEntity.class);
              entity.setId(appUserId);
              return entity;
            });
    when(externalIdentities.findFirstByAppUserIdAndProvider(
            appUserId, IdentityProviderType.KEYCLOAK))
        .thenReturn(Optional.empty());
    when(externalIdentities.save(any(AppUserExternalIdentityJpaEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adapter.save(
        AppUser.createNew(
            null,
            KeycloakUserSub.of(keycloakId),
            "cashier",
            "cashier@example.com",
            null,
            null,
            null,
            "Cashier",
            null,
            Instant.parse("2026-06-12T00:00:00Z")));

    var captor = ArgumentCaptor.forClass(AppUserExternalIdentityJpaEntity.class);
    Mockito.verify(externalIdentities).save(captor.capture());
    assertThat(captor.getValue().getAppUserId()).isEqualTo(appUserId);
    assertThat(captor.getValue().getProvider()).isEqualTo(IdentityProviderType.KEYCLOAK);
    assertThat(captor.getValue().getIssuer()).isEqualTo("legacy:keycloak");
    assertThat(captor.getValue().getExternalSubject()).isEqualTo(keycloakId.toString());
  }

  private static AppUserJpaEntity activeUser(UUID id) {
    var entity = new AppUserJpaEntity();
    entity.setId(id);
    entity.setUsername("cashier");
    entity.setEmail("cashier@example.com");
    entity.setStatus(com.tchalanet.server.platform.identity.api.model.UserStatus.ACTIVE);
    return entity;
  }
}
