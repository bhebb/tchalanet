package com.tchalanet.server.platform.identity.internal.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.UserRecord;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.ProvisionExternalUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SuppressWarnings("unchecked")
class FirebaseUserProvisionServiceTest {

  private final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
  private final ObjectProvider<FirebaseAuth> provider = mock(ObjectProvider.class);
  private final FirebaseIdentityProperties properties =
      new FirebaseIdentityProperties("test-project", null, null, null);
  private final FirebaseUserProvisionService service =
      new FirebaseUserProvisionService(provider, properties);

  @Test
  void createsAnEnabledFirebaseUser() throws Exception {
    var created = mock(UserRecord.class);
    var notFound = mock(FirebaseAuthException.class);
    when(provider.getIfAvailable()).thenReturn(firebaseAuth);
    when(notFound.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
    when(firebaseAuth.getUserByEmail("admin@example.test")).thenThrow(notFound);
    when(firebaseAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(created);
    when(created.getUid()).thenReturn("firebase-uid");

    var result =
        service.provisionUser(null, "admin@example.test", null, "Admin", null);

    assertThat(result.uid()).isEqualTo("firebase-uid");
    assertThat(result.created()).isTrue();
    verify(firebaseAuth).createUser(any(UserRecord.CreateRequest.class));
  }

  @Test
  void exposesProviderNeutralProvisioningResult() throws Exception {
    var created = mock(UserRecord.class);
    var notFound = mock(FirebaseAuthException.class);
    when(provider.getIfAvailable()).thenReturn(firebaseAuth);
    when(notFound.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
    when(firebaseAuth.getUserByEmail("admin@example.test")).thenThrow(notFound);
    when(firebaseAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(created);
    when(created.getUid()).thenReturn("firebase-uid");

    var result =
        service.provisionUser(
            new ProvisionExternalUserRequest(
                null, "admin@example.test", null, "Admin", null));

    assertThat(result.provider()).isEqualTo(IdentityProviderType.FIREBASE);
    assertThat(result.issuer()).isEqualTo("https://securetoken.google.com/test-project");
    assertThat(result.externalSubject()).isEqualTo("firebase-uid");
    assertThat(result.created()).isTrue();
  }

  @Test
  void requiresEmailOrPhone() {
    assertThatThrownBy(() -> service.provisionUser(null, null, null, "Admin", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email or phone");
  }

  @Test
  void reusesAnExistingFirebaseIdentity() throws Exception {
    var existing = mock(UserRecord.class);
    when(provider.getIfAvailable()).thenReturn(firebaseAuth);
    when(firebaseAuth.getUserByEmail("admin@example.test")).thenReturn(existing);
    when(existing.getUid()).thenReturn("existing-uid");

    var result =
        service.provisionUser(null, "admin@example.test", null, "Admin", null);

    assertThat(result.uid()).isEqualTo("existing-uid");
    assertThat(result.created()).isFalse();
    verify(firebaseAuth, never()).createUser(any(UserRecord.CreateRequest.class));
  }

  @Test
  void deletesANewFirebaseIdentityWhenTheLocalTransactionRollsBack() throws Exception {
    var created = mock(UserRecord.class);
    var notFound = mock(FirebaseAuthException.class);
    when(provider.getIfAvailable()).thenReturn(firebaseAuth);
    when(notFound.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
    when(firebaseAuth.getUserByEmail("rollback@example.test")).thenThrow(notFound);
    when(firebaseAuth.createUser(any(UserRecord.CreateRequest.class))).thenReturn(created);
    when(created.getUid()).thenReturn("rollback-uid");
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.provisionUser(null, "rollback@example.test", null, "Rollback", null);

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

      verify(firebaseAuth).deleteUser("rollback-uid");
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void failsClearlyWhenFirebaseProviderIsNotActive() {
    when(provider.getIfAvailable()).thenReturn(null);

    assertThatThrownBy(
            () -> service.provisionUser(null, "admin@example.test", null, "Admin", null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("tch.identity.provider=firebase");
  }
}
