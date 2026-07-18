package com.tchalanet.server.platform.identity.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.error.IdentityErrorCodes;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class PublicLoginIdentifierServiceTest {
  private static final UserId USER_ID =
      UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000101"));

  private final AppUserJpaAdapter users = Mockito.mock(AppUserJpaAdapter.class);
  private final PublicLoginIdentifierService service = new PublicLoginIdentifierService(users);

  @Test
  void resolvesActiveUsernameToNormalizedEmail() {
    when(users.findByNormalizedUsername("admin"))
        .thenReturn(Optional.of(user("admin", "Admin@Example.COM", UserStatus.ACTIVE)));
    when(users.findExternalSubject(USER_ID, IdentityProviderType.FIREBASE))
        .thenReturn(Optional.of("firebase-subject"));

    assertThat(service.resolveIdentifier(" Admin ")).isEqualTo("admin@example.com");
  }

  @Test
  void rejectsUnknownUsernameWithGenericFailure() {
    when(users.findByNormalizedUsername("missing")).thenReturn(Optional.empty());

    assertGenericFailure(() -> service.resolveIdentifier("missing"), HttpStatus.FORBIDDEN);
  }

  @Test
  void rejectsInactiveUsernameWithGenericFailure() {
    when(users.findByNormalizedUsername("admin"))
        .thenReturn(Optional.of(user("admin", "admin@example.com", UserStatus.SUSPENDED)));

    assertGenericFailure(() -> service.resolveIdentifier("admin"), HttpStatus.FORBIDDEN);
  }

  @Test
  void rejectsEmailInputBecauseEmailBypassesLookup() {
    assertGenericFailure(() -> service.resolveIdentifier("admin@example.com"), HttpStatus.FORBIDDEN);
  }

  @Test
  void rejectsUsernameWithoutFirebaseIdentityWithGenericFailure() {
    when(users.findByNormalizedUsername("admin"))
        .thenReturn(Optional.of(user("admin", "admin@example.com", UserStatus.ACTIVE)));
    when(users.findExternalSubject(USER_ID, IdentityProviderType.FIREBASE)).thenReturn(Optional.empty());

    assertGenericFailure(() -> service.resolveIdentifier("admin"), HttpStatus.FORBIDDEN);
  }

  private static AppUser user(String username, String email, UserStatus status) {
    return new AppUser(
        USER_ID, null, username, email, null, null, null, null, null, status, null, null, null, false,
        false, null, null);
  }

  private static void assertGenericFailure(Runnable action, HttpStatus status) {
    assertThatThrownBy(action::run)
        .isInstanceOf(ProblemRestException.class)
        .satisfies(
            error -> {
              var problem = ((ProblemRestException) error).getProblem();
              assertThat(problem.getStatus()).isEqualTo(status.value());
              assertThat(problem.getDetail()).isEqualTo("Request could not be completed");
              assertThat(problem.getProperties())
                  .containsEntry("code", IdentityErrorCodes.AUTH_INVALID_CREDENTIALS.code())
                  .containsEntry(
                      "category", IdentityErrorCodes.AUTH_INVALID_CREDENTIALS.category().wireValue())
                  .doesNotContainKey("username");
            });
  }
}
