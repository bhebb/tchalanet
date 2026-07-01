package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserExternalIdentityJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserExternalIdentityJpaRepository;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserJpaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ExternalIdentityAppUserResolver {

  static final String LEGACY_KEYCLOAK_ISSUER = "legacy:keycloak";
  private static final Pattern E164_PHONE = Pattern.compile("^\\+[1-9]\\d{7,14}$");

  private final AppUserExternalIdentityJpaRepository externalIdentities;
  private final AppUserJpaRepository appUsers;
  private final UserBootstrapProperties bootstrapProperties;
  private final AuditApi auditApi;
  private final TchContextResolver contextResolver;

  @Transactional
  public Optional<AppUserIdentityResolution> resolve(ExternalAuthenticatedUser externalUser) {
    var mapping =
        externalIdentities
            .findByProviderAndIssuerAndExternalSubject(
                externalUser.provider(), externalUser.issuer(), externalUser.subject())
            .or(() -> claimLegacyKeycloakMapping(externalUser))
            .or(() -> bootstrapMapping(externalUser));

    return mapping.flatMap(
        externalIdentity ->
            appUsers
                .findById(externalIdentity.getAppUserId())
                .filter(appUser -> appUser.getDeletedAt() == null)
                .map(appUser -> new AppUserIdentityResolution(appUser.getId(), appUser.getStatus())));
  }

  private Optional<AppUserExternalIdentityJpaEntity> bootstrapMapping(
      ExternalAuthenticatedUser externalUser) {
    var mapping = switch (bootstrapProperties.effectiveMode()) {
      case DENY -> Optional.<AppUserExternalIdentityJpaEntity>empty();
      case INVITE_ONLY -> linkPreprovisionedUser(externalUser, UserStatus.INVITED);
      case ADMIN_PREPROVISIONED -> linkPreprovisionedUser(externalUser, UserStatus.ACTIVE);
      case CONTROLLED_AUTO -> createControlledUser(externalUser);
    };
    if (mapping.isEmpty()) {
      audit(externalUser, null, AuditAction.APP_USER_BOOTSTRAP_DENIED, "policy_denied");
    }
    return mapping;
  }

  private Optional<AppUserExternalIdentityJpaEntity> linkPreprovisionedUser(
      ExternalAuthenticatedUser externalUser, UserStatus requiredStatus) {
    var candidate = findPreprovisionedUser(externalUser);
    if (candidate.isEmpty()) {
      return Optional.empty();
    }
    return candidate
        .filter(appUser -> appUser.getDeletedAt() == null)
        .filter(appUser -> requiredStatus == null || appUser.getStatus() == requiredStatus)
        .map(
            appUser -> {
              if (requiredStatus == UserStatus.INVITED) {
                appUser.setStatus(UserStatus.PENDING_APPROVAL);
                appUsers.save(appUser);
              }
              var mapping = saveMapping(appUser.getId(), externalUser);
              audit(
                  externalUser,
                  appUser.getId(),
                  requiredStatus == UserStatus.INVITED
                      ? AuditAction.APP_USER_BOOTSTRAP_INVITE_CONSUMED
                      : AuditAction.APP_USER_EXTERNAL_IDENTITY_LINKED,
                  "linked");
              return mapping;
            });
  }

  private Optional<AppUserJpaEntity> findPreprovisionedUser(
      ExternalAuthenticatedUser externalUser) {
    if (externalUser.emailVerified()
        && externalUser.email() != null
        && !externalUser.email().isBlank()) {
      var byEmail = appUsers.findByEmail(externalUser.email());
      if (byEmail.isPresent()) {
        return byEmail;
      }
    }
    return verifiedFirebasePhone(externalUser).flatMap(appUsers::findByPhone);
  }

  private static Optional<String> verifiedFirebasePhone(ExternalAuthenticatedUser externalUser) {
    if (externalUser.provider() != IdentityProviderType.FIREBASE) {
      return Optional.empty();
    }
    var firebaseClaim = externalUser.safeClaims().get("firebase");
    if (!(firebaseClaim instanceof Map<?, ?> firebase)
        || !"phone".equals(firebase.get("sign_in_provider"))) {
      return Optional.empty();
    }
    var phone = externalUser.safeClaims().get("phone_number");
    if (!(phone instanceof String value) || !E164_PHONE.matcher(value).matches()) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  private Optional<AppUserExternalIdentityJpaEntity> createControlledUser(
      ExternalAuthenticatedUser externalUser) {
    if (!externalUser.emailVerified()
        || !bootstrapProperties.controlledAutoAllows(externalUser.email())
        || appUsers.findByEmail(externalUser.email()).isPresent()) {
      return Optional.empty();
    }
    var appUser = new AppUserJpaEntity();
    appUser.setEmail(externalUser.email());
    appUser.setUsername(externalUser.email());
    appUser.setDisplayName(externalUser.email());
    appUser.setStatus(UserStatus.PENDING_APPROVAL);
    var saved = appUsers.save(appUser);
    var mapping = saveMapping(saved.getId(), externalUser);
    audit(
        externalUser,
        saved.getId(),
        AuditAction.APP_USER_BOOTSTRAP_CREATED,
        "pending_approval_created");
    return Optional.of(mapping);
  }

  private AppUserExternalIdentityJpaEntity saveMapping(
      UUID appUserId, ExternalAuthenticatedUser externalUser) {
    var mapping = new AppUserExternalIdentityJpaEntity();
    mapping.setAppUserId(appUserId);
    mapping.setProvider(externalUser.provider());
    mapping.setIssuer(externalUser.issuer());
    mapping.setExternalSubject(externalUser.subject());
    mapping.setEmailSnapshot(externalUser.email());
    return externalIdentities.save(mapping);
  }

  private void audit(
      ExternalAuthenticatedUser externalUser,
      UUID appUserId,
      AuditAction action,
      String reasonCode) {
    var ctx = contextResolver.currentOrNull();
    var tenantUuid = ctx != null ? ctx.tenantUuid() : null;
    auditApi.logAuditEvent(
        new LogAuditEventRequest(
            AuditEntityType.USER,
            appUserId == null ? externalSubjectReference(externalUser.subject()) : appUserId.toString(),
            action,
            Map.of(
                "provider", externalUser.provider().name(),
                "issuer", externalUser.issuer(),
                "externalSubjectRef", externalSubjectReference(externalUser.subject()),
                "emailSnapshot", externalUser.email() == null ? "" : externalUser.email(),
                "phoneLinked", verifiedFirebasePhone(externalUser).isPresent(),
                "bootstrapMode", bootstrapProperties.effectiveMode().name(),
                "reasonCode", reasonCode),
            tenantUuid));
  }

  private static String externalSubjectReference(String subject) {
    try {
      var digest = MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest, 0, 12);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is required", ex);
    }
  }

  @Transactional
  public void touchLastLogin(UUID appUserId) {
    appUsers.touchLastLogin(appUserId, Instant.now());
  }

  private Optional<AppUserExternalIdentityJpaEntity> claimLegacyKeycloakMapping(
      ExternalAuthenticatedUser externalUser) {
    if (externalUser.provider() != IdentityProviderType.KEYCLOAK) {
      return Optional.empty();
    }

    return externalIdentities
        .findByProviderAndIssuerAndExternalSubject(
            IdentityProviderType.KEYCLOAK, LEGACY_KEYCLOAK_ISSUER, externalUser.subject())
        .map(
            legacy -> {
              legacy.setIssuer(externalUser.issuer());
              legacy.setEmailSnapshot(externalUser.email());
              return externalIdentities.save(legacy);
            });
  }
}
