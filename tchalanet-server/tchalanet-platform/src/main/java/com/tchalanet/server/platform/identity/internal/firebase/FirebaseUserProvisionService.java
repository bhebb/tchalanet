package com.tchalanet.server.platform.identity.internal.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.UserRecord;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.IdentityProvisioningApi;
import com.tchalanet.server.platform.identity.api.ProvisionExternalUserRequest;
import com.tchalanet.server.platform.identity.api.ProvisionedExternalUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@ConditionalOnExpression(
    "'${tch.identity.provider:firebase}' == 'firebase' || '${tch.identity.provider:firebase}' == 'firebase-emulator'")
@RequiredArgsConstructor
@Slf4j
public class FirebaseUserProvisionService implements IdentityProvisioningApi {

  private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
  private final FirebaseIdentityProperties firebaseIdentityProperties;

  @Override
  public ProvisionedExternalUser provisionUser(ProvisionExternalUserRequest request) {
    var result =
        provisionUser(
            request.requestedExternalSubject(),
            request.email(),
            request.phone(),
            request.displayName(),
            request.initialPassword());
    return new ProvisionedExternalUser(
        IdentityProviderType.FIREBASE,
        firebaseIdentityProperties.issuer(),
        result.uid(),
        result.created());
  }

  public FirebaseProvisionResult provisionUser(
      String uid,
      String email,
      String phone,
      String displayName,
      String password) {
    requireEmailOrPhone(email, phone);
    var firebaseAuth = requiredFirebaseAuth();

    var existing = findExisting(firebaseAuth, uid, email, phone);
    if (existing != null) {
      return new FirebaseProvisionResult(existing.getUid(), false);
    }

    var request = new UserRecord.CreateRequest().setDisabled(false);
    if (uid != null && !uid.isBlank()) {
      request.setUid(uid.trim());
    }
    if (email != null && !email.isBlank()) {
      request.setEmail(email.trim());
    }
    if (phone != null && !phone.isBlank()) {
      request.setPhoneNumber(phone.trim());
    }
    if (displayName != null && !displayName.isBlank()) {
      request.setDisplayName(displayName.trim());
    }
    if (password != null && !password.isBlank()) {
      request.setPassword(password);
    }

    try {
      var created = firebaseAuth.createUser(request);
      registerRollbackCompensation(firebaseAuth, created.getUid());
      log.info(
          "Firebase user created uid={} emailPresent={} phonePresent={}",
          created.getUid(),
          email != null && !email.isBlank(),
          phone != null && !phone.isBlank());
      return new FirebaseProvisionResult(created.getUid(), true);
    } catch (FirebaseAuthException ex) {
      throw new IllegalStateException("Firebase user could not be created", ex);
    }
  }

  public void resetPasswordForUid(String uid, String newPassword) {
    var firebaseAuth = requiredFirebaseAuth();
    try {
      firebaseAuth.updateUser(new UserRecord.UpdateRequest(uid).setPassword(newPassword));
      log.info("Firebase password reset uid={}", uid);
    } catch (FirebaseAuthException ex) {
      throw new IllegalStateException("Firebase password reset failed for uid=" + uid, ex);
    }
  }

  private UserRecord findExisting(FirebaseAuth firebaseAuth, String uid, String email, String phone) {
    if (uid != null && !uid.isBlank()) {
      var byUid = find(() -> firebaseAuth.getUser(uid.trim()));
      if (byUid != null) {
        return byUid;
      }
    }
    if (email != null && !email.isBlank()) {
      var byEmail = find(() -> firebaseAuth.getUserByEmail(email.trim()));
      if (byEmail != null) {
        return byEmail;
      }
    }
    if (phone != null && !phone.isBlank()) {
      return find(() -> firebaseAuth.getUserByPhoneNumber(phone.trim()));
    }
    return null;
  }

  private UserRecord find(FirebaseLookup lookup) {
    try {
      return lookup.get();
    } catch (FirebaseAuthException ex) {
      if (ex.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
        return null;
      }
      throw new IllegalStateException("Firebase user lookup failed", ex);
    }
  }

  private void registerRollbackCompensation(FirebaseAuth firebaseAuth, String uid) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              deleteCreatedUser(firebaseAuth, uid);
            }
          }
        });
  }

  private void deleteCreatedUser(FirebaseAuth firebaseAuth, String uid) {
    try {
      firebaseAuth.deleteUser(uid);
      log.warn("Deleted Firebase user uid={} after local transaction rollback", uid);
    } catch (FirebaseAuthException ex) {
      log.error("Could not compensate Firebase user uid={} after local rollback", uid, ex);
    }
  }

  private FirebaseAuth requiredFirebaseAuth() {
    var firebaseAuth = firebaseAuthProvider.getIfAvailable();
    if (firebaseAuth == null) {
      throw new IllegalStateException(
          "Firebase user provisioning requires tch.identity.provider=firebase or firebase-emulator");
    }
    return firebaseAuth;
  }

  private static void requireEmailOrPhone(String email, String phone) {
    if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
      throw new IllegalArgumentException("Either email or phone must be provided");
    }
  }

  @FunctionalInterface
  private interface FirebaseLookup {
    UserRecord get() throws FirebaseAuthException;
  }

  public record FirebaseProvisionResult(String uid, boolean created) {}
}
