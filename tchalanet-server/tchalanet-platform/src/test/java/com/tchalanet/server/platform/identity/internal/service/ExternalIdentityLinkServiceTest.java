package com.tchalanet.server.platform.identity.internal.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserExternalIdentityJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserExternalIdentityJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExternalIdentityLinkServiceTest {

  private final AppUserExternalIdentityJpaRepository repository =
      mock(AppUserExternalIdentityJpaRepository.class);
  private final ExternalIdentityLinkService service = new ExternalIdentityLinkService(repository);

  @Test
  void persistsANewFirebaseLink() {
    var userId = UserId.of(UUID.randomUUID());
    when(repository.findByProviderAndIssuerAndExternalSubject(
            IdentityProviderType.FIREBASE, "issuer", "firebase-uid"))
        .thenReturn(Optional.empty());

    service.link(userId, IdentityProviderType.FIREBASE, "issuer", "firebase-uid", "a@example.test");

    verify(repository).save(any(AppUserExternalIdentityJpaEntity.class));
  }

  @Test
  void refusesToMoveAnIdentityToAnotherAppUser() {
    var identity = new AppUserExternalIdentityJpaEntity();
    identity.setAppUserId(UUID.randomUUID());
    when(repository.findByProviderAndIssuerAndExternalSubject(
            IdentityProviderType.FIREBASE, "issuer", "firebase-uid"))
        .thenReturn(Optional.of(identity));

    assertThatThrownBy(
            () ->
                service.link(
                    UserId.of(UUID.randomUUID()),
                    IdentityProviderType.FIREBASE,
                    "issuer",
                    "firebase-uid",
                    "a@example.test"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already linked");
  }
}
