package com.tchalanet.server.platform.identity.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.platform.identity.api.ExternalAuthenticatedUser;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserExternalIdentityJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserExternalIdentityJpaRepository;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserJpaRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalIdentityAppUserResolverTest {

  @Mock private AppUserExternalIdentityJpaRepository externalIdentities;
  @Mock private AppUserJpaRepository appUsers;
  @Mock private AuditApi auditApi;
  @Mock private TchContextResolver contextResolver;

  private ExternalIdentityAppUserResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = resolver(AppUserBootstrapMode.DENY);
  }

  @Test
  void resolvesKnownExternalIdentityToActiveAppUser() {
    var externalUser = externalUser(IdentityProviderType.FIREBASE, "firebase-subject");
    var appUserId = UUID.randomUUID();
    var mapping = mapping(appUserId, externalUser);
    var appUser = appUser(appUserId, UserStatus.ACTIVE);
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.of(mapping));
    when(appUsers.findById(appUserId)).thenReturn(Optional.of(appUser));

    var result = resolver.resolve(externalUser);

    assertThat(result)
        .contains(new AppUserIdentityResolution(appUserId, UserStatus.ACTIVE));
  }

  @Test
  void claimsLegacyKeycloakMappingForVerifiedIssuer() {
    var externalUser = externalUser(IdentityProviderType.KEYCLOAK, "keycloak-subject");
    var appUserId = UUID.randomUUID();
    var legacy = mapping(appUserId, externalUser);
    legacy.setIssuer(ExternalIdentityAppUserResolver.LEGACY_KEYCLOAK_ISSUER);
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            IdentityProviderType.KEYCLOAK,
            ExternalIdentityAppUserResolver.LEGACY_KEYCLOAK_ISSUER,
            externalUser.subject()))
        .thenReturn(Optional.of(legacy));
    when(externalIdentities.save(legacy)).thenReturn(legacy);
    when(appUsers.findById(appUserId)).thenReturn(Optional.of(appUser(appUserId, UserStatus.ACTIVE)));

    assertThat(resolver.resolve(externalUser)).isPresent();
    assertThat(legacy.getIssuer()).isEqualTo(externalUser.issuer());
    verify(externalIdentities).save(legacy);
  }

  @Test
  void deniesUnknownNonKeycloakExternalIdentity() {
    var externalUser = externalUser(IdentityProviderType.FIREBASE, "unknown");
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());

    assertThat(resolver.resolve(externalUser)).isEmpty();
    verify(appUsers, never()).findById(org.mockito.ArgumentMatchers.any());
    verify(auditApi)
        .logAuditEvent(
            org.mockito.ArgumentMatchers.argThat(
                event -> event.action() == AuditAction.APP_USER_BOOTSTRAP_DENIED));
  }

  @ParameterizedTest
  @EnumSource(value = AppUserBootstrapMode.class, names = {"INVITE_ONLY", "ADMIN_PREPROVISIONED"})
  void linksVerifiedExternalIdentityToEligiblePreprovisionedUser(AppUserBootstrapMode mode) {
    var externalUser = externalUser(IdentityProviderType.FIREBASE, "firebase-subject");
    var appUserId = UUID.randomUUID();
    var status = mode == AppUserBootstrapMode.INVITE_ONLY ? UserStatus.INVITED : UserStatus.ACTIVE;
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());
    when(appUsers.findByEmail(externalUser.email()))
        .thenReturn(Optional.of(appUser(appUserId, status)));
    if (mode == AppUserBootstrapMode.INVITE_ONLY) {
      when(appUsers.save(org.mockito.ArgumentMatchers.any(AppUserJpaEntity.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
    }
    when(externalIdentities.save(anyMapping())).thenAnswer(invocation -> invocation.getArgument(0));
    var resolvedStatus =
        mode == AppUserBootstrapMode.INVITE_ONLY ? UserStatus.PENDING_APPROVAL : status;
    when(appUsers.findById(appUserId)).thenReturn(Optional.of(appUser(appUserId, resolvedStatus)));

    var result = resolver(mode).resolve(externalUser);

    assertThat(result).contains(new AppUserIdentityResolution(appUserId, resolvedStatus));
    verify(externalIdentities).save(anyMapping());
    verify(auditApi)
        .logAuditEvent(
            org.mockito.ArgumentMatchers.argThat(
                event ->
                    event.action()
                        == (mode == AppUserBootstrapMode.INVITE_ONLY
                            ? AuditAction.APP_USER_BOOTSTRAP_INVITE_CONSUMED
                            : AuditAction.APP_USER_EXTERNAL_IDENTITY_LINKED)));
  }

  @Test
  void inviteOnlyDoesNotLinkNonInvitedUser() {
    var externalUser = externalUser(IdentityProviderType.FIREBASE, "firebase-subject");
    var appUserId = UUID.randomUUID();
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());
    when(appUsers.findByEmail(externalUser.email()))
        .thenReturn(Optional.of(appUser(appUserId, UserStatus.ACTIVE)));

    assertThat(resolver(AppUserBootstrapMode.INVITE_ONLY).resolve(externalUser)).isEmpty();
    verify(externalIdentities, never()).save(anyMapping());
  }

  @Test
  void adminPreprovisionedLinksActiveUserForImmediateAccess() {
    var externalUser = externalUser(IdentityProviderType.FIREBASE, "firebase-subject");
    var appUserId = UUID.randomUUID();
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());
    when(appUsers.findByEmail(externalUser.email()))
        .thenReturn(Optional.of(appUser(appUserId, UserStatus.ACTIVE)));
    when(externalIdentities.save(anyMapping())).thenAnswer(invocation -> invocation.getArgument(0));
    when(appUsers.findById(appUserId))
        .thenReturn(Optional.of(appUser(appUserId, UserStatus.ACTIVE)));

    assertThat(resolver(AppUserBootstrapMode.ADMIN_PREPROVISIONED).resolve(externalUser))
        .contains(new AppUserIdentityResolution(appUserId, UserStatus.ACTIVE));
  }

  @Test
  void adminPreprovisionedLinksActiveUserByVerifiedFirebasePhone() {
    var externalUser = firebasePhoneUser("+50937000000", "phone");
    var appUserId = UUID.randomUUID();
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());
    when(appUsers.findByPhone("+50937000000"))
        .thenReturn(Optional.of(appUser(appUserId, UserStatus.ACTIVE)));
    when(externalIdentities.save(anyMapping())).thenAnswer(invocation -> invocation.getArgument(0));
    when(appUsers.findById(appUserId))
        .thenReturn(Optional.of(appUser(appUserId, UserStatus.ACTIVE)));

    assertThat(resolver(AppUserBootstrapMode.ADMIN_PREPROVISIONED).resolve(externalUser))
        .contains(new AppUserIdentityResolution(appUserId, UserStatus.ACTIVE));
    verify(appUsers).findByPhone("+50937000000");
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"password", "custom", "google.com"})
  void adminPreprovisionedDoesNotTrustPhoneFromNonPhoneFirebaseSignIn(String signInProvider) {
    var externalUser = firebasePhoneUser("+50937000000", signInProvider);
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());

    assertThat(resolver(AppUserBootstrapMode.ADMIN_PREPROVISIONED).resolve(externalUser)).isEmpty();
    verify(appUsers, never()).findByPhone(org.mockito.ArgumentMatchers.any());
  }

  @ParameterizedTest
  @EnumSource(
      value = UserStatus.class,
      names = {"INVITED", "PENDING_APPROVAL", "SUSPENDED"})
  void adminPreprovisionedDoesNotLinkNonActiveUser(UserStatus status) {
    var externalUser = externalUser(IdentityProviderType.FIREBASE, "firebase-subject");
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());
    when(appUsers.findByEmail(externalUser.email()))
        .thenReturn(Optional.of(appUser(UUID.randomUUID(), status)));

    assertThat(resolver(AppUserBootstrapMode.ADMIN_PREPROVISIONED).resolve(externalUser)).isEmpty();
    verify(externalIdentities, never()).save(anyMapping());
  }

  @Test
  void controlledAutoCreatesPendingUserOnlyForAllowlistedVerifiedEmail() {
    var externalUser = externalUser(IdentityProviderType.FIREBASE, "firebase-subject");
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());
    when(appUsers.save(org.mockito.ArgumentMatchers.any(AppUserJpaEntity.class)))
        .thenAnswer(
            invocation -> {
              var user = invocation.<AppUserJpaEntity>getArgument(0);
              user.setId(UUID.randomUUID());
              return user;
            });
    when(externalIdentities.save(anyMapping())).thenAnswer(invocation -> invocation.getArgument(0));
    when(appUsers.findById(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation ->
                Optional.of(appUser(invocation.<UUID>getArgument(0), UserStatus.PENDING_APPROVAL)));

    var result = controlledAutoResolver().resolve(externalUser);

    assertThat(result).get().extracting(AppUserIdentityResolution::status)
        .isEqualTo(UserStatus.PENDING_APPROVAL);
    verify(appUsers).save(org.mockito.ArgumentMatchers.any(AppUserJpaEntity.class));
    verify(auditApi)
        .logAuditEvent(
            org.mockito.ArgumentMatchers.argThat(
                event ->
                    event.action() == AuditAction.APP_USER_BOOTSTRAP_CREATED
                        && !event.details().get("externalSubjectRef").equals(externalUser.subject())));
  }

  @Test
  void preprovisionedModeRequiresVerifiedEmail() {
    var externalUser =
        new ExternalAuthenticatedUser(
            IdentityProviderType.FIREBASE,
            "https://issuer.example",
            "firebase-subject",
            "user@example.com",
            false,
            Map.of());
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());

    assertThat(resolver(AppUserBootstrapMode.ADMIN_PREPROVISIONED).resolve(externalUser)).isEmpty();
    verify(appUsers, never()).findByEmail(externalUser.email());
  }

  @Test
  void controlledAutoDoesNotDuplicateExistingAppUser() {
    var externalUser = externalUser(IdentityProviderType.FIREBASE, "firebase-subject");
    when(externalIdentities.findByProviderAndIssuerAndExternalSubject(
            externalUser.provider(), externalUser.issuer(), externalUser.subject()))
        .thenReturn(Optional.empty());
    when(appUsers.findByEmail(externalUser.email()))
        .thenReturn(Optional.of(appUser(UUID.randomUUID(), UserStatus.ACTIVE)));

    assertThat(controlledAutoResolver().resolve(externalUser)).isEmpty();
    verify(appUsers, never()).save(org.mockito.ArgumentMatchers.any());
  }

  private ExternalIdentityAppUserResolver resolver(AppUserBootstrapMode mode) {
    return new ExternalIdentityAppUserResolver(
        externalIdentities,
        appUsers,
        new UserBootstrapProperties(true, false, mode, java.util.List.of(), java.util.List.of(), false),
        auditApi,
        contextResolver);
  }

  private ExternalIdentityAppUserResolver controlledAutoResolver() {
    return new ExternalIdentityAppUserResolver(
        externalIdentities,
        appUsers,
        new UserBootstrapProperties(
            true,
            false,
            AppUserBootstrapMode.CONTROLLED_AUTO,
            java.util.List.of("user@example.com"),
            java.util.List.of(),
            false),
        auditApi,
        contextResolver);
  }

  private static AppUserExternalIdentityJpaEntity anyMapping() {
    return org.mockito.ArgumentMatchers.any(AppUserExternalIdentityJpaEntity.class);
  }

  private static ExternalAuthenticatedUser externalUser(
      IdentityProviderType provider, String subject) {
    return new ExternalAuthenticatedUser(
        provider, "https://issuer.example", subject, "user@example.com", true, Map.of());
  }

  private static ExternalAuthenticatedUser firebasePhoneUser(String phone, String signInProvider) {
    return new ExternalAuthenticatedUser(
        IdentityProviderType.FIREBASE,
        "https://issuer.example",
        "firebase-phone-subject",
        null,
        false,
        Map.of(
            "phone_number", phone,
            "firebase", Map.of("sign_in_provider", signInProvider)));
  }

  private static AppUserExternalIdentityJpaEntity mapping(
      UUID appUserId, ExternalAuthenticatedUser externalUser) {
    var mapping = new AppUserExternalIdentityJpaEntity();
    mapping.setAppUserId(appUserId);
    mapping.setProvider(externalUser.provider());
    mapping.setIssuer(externalUser.issuer());
    mapping.setExternalSubject(externalUser.subject());
    return mapping;
  }

  private static AppUserJpaEntity appUser(UUID appUserId, UserStatus status) {
    var appUser = new AppUserJpaEntity();
    appUser.setId(appUserId);
    appUser.setStatus(status);
    return appUser;
  }
}
