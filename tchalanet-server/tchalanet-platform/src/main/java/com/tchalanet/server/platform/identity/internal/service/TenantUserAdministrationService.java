package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import com.tchalanet.server.platform.identity.api.IdentityProvisioningApi;
import com.tchalanet.server.platform.identity.api.ProvisionExternalUserRequest;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import com.tchalanet.server.platform.identity.internal.model.UserPreference;
import com.tchalanet.server.platform.identity.internal.model.UserRow;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.UserPreferenceJpaAdapter;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantUserAdministrationService {

  private final AppUserJpaAdapter users;
  private final UserPreferenceJpaAdapter preferences;
  private final TimeProvider timeProvider;
  private final IdentityProvisioningApi identityProvisioning;
  private final ExternalIdentityLinkService externalIdentityLinks;
  private final TemporaryCredentialService temporaryCredentials;

  @Transactional
  public CreateUserResult createUser(
      String email,
      String phone,
      String firstName,
      String lastName,
      com.tchalanet.server.catalog.theme.api.ThemeMode prefThemeMode,
      Short prefDensity,
      String prefLocale,
      String prefTimeZone,
      String prefCurrency,
      boolean sendInvitation,
      Set<String> initialRoles) {
    if (users.findByEmailOrPhone(email, phone).isPresent()) {
      throw new IllegalStateException("User already exists with this email or phone");
    }

    var username = resolveUsername(email, phone);
    var externalUser =
        identityProvisioning.provisionUser(
            new ProvisionExternalUserRequest(
                null, email, phone, buildDisplayName(firstName, lastName), null));

    var now = timeProvider.nowInstant();
    var user =
        users.save(
            AppUser.createNew(
                null,
                null,
                username,
                email,
                phone,
                firstName,
                lastName,
                buildDisplayName(firstName, lastName),
                null,
                now).reactivate());
    externalIdentityLinks.link(
        user.id(),
        externalUser.provider(),
        externalUser.issuer(),
        externalUser.externalSubject(),
        email);
    preferences.upsert(UserPreference.forUser(user.id()).applyOverrides(prefThemeMode, prefDensity, parseLocale(prefLocale), parseZone(prefTimeZone), parseCurrency(prefCurrency)));
    return new CreateUserResult(user.id());
  }

  /**
   * Creates a Firebase-authenticated user for a tenant provisioning flow.
   * Tenant authorization remains entirely in Tchalanet; tenantCode is retained for API
   * compatibility while callers migrate.
   */
  @Transactional
  public CreateUserResult createUserForTenant(
      String email,
      String phone,
      String firstName,
      String lastName,
      String tenantCode) {
    var existing = users.findByEmailOrPhone(email, phone);
    if (existing.isPresent()) {
      var now = timeProvider.nowInstant();
      var saved = users.save(existing.get().requireFirstLoginActivation(now));
      return new CreateUserResult(saved.id(), false, false, null);
    }
    var username = resolveUsername(email, phone);
    var temporaryPassword = temporaryCredentials.adminTemporaryCredentialsEnabled()
        ? temporaryCredentials.adminTemporaryPassword()
        : null;
    var externalUser =
        identityProvisioning.provisionUser(
            new ProvisionExternalUserRequest(
                null, email, phone, buildDisplayName(firstName, lastName), temporaryPassword));
    var now = timeProvider.nowInstant();
    var user = users.save(AppUser.createNew(
        null, null, username, email, phone,
        firstName, lastName, buildDisplayName(firstName, lastName), null, now)
        .reactivate()
        .requireFirstLoginActivation(now));
    externalIdentityLinks.link(
        user.id(),
        externalUser.provider(),
        externalUser.issuer(),
        externalUser.externalSubject(),
        email);
    var credentialIssued = temporaryPassword != null && externalUser.created();
    return new CreateUserResult(user.id(), true, credentialIssued, credentialIssued ? temporaryPassword : null);
  }

  @Transactional
  public FirstLoginActivationResult completeFirstLogin(
      UserId userId, String firstName, String lastName, String phone) {
    var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    var saved = users.save(user.completeFirstLogin(firstName, lastName, phone, timeProvider.nowInstant()));
    return new FirstLoginActivationResult(
        saved.id(),
        saved.mustChangePassword(),
        saved.mustCompleteProfile(),
        "/app/admin");
  }

  public void approveUser(UserId userId, UserId approvedBy) {
    var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    users.save(user.approve(timeProvider.nowInstant(), approvedBy));
  }

  public void suspendUser(UserId userId) {
    var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    users.save(user.suspend());
  }

  public void reactivateUser(UserId userId) {
    var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    users.save(user.reactivate());
  }

  public void deleteUser(UserId userId) {
    users.softDelete(userId, timeProvider.nowInstant());
  }

  public UserProfile profile(UserId userId) {
    var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    return new UserProfile(
        user.id(),
        user.keycloakSub(),
        user.username(),
        user.email(),
        user.phone(),
        user.status().name(),
        user.firstName(),
        user.lastName(),
        user.displayName());
  }

  public void sendInvitation(UserId userId) {
    users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
  }

  public TchPage<UserRow> listAll(TchPageRequest pageRequest) {
    return toPage(users.findAll(pageRequest.pageable()));
  }

  public TchPage<UserRow> listByTenant(TenantId tenantId, TchPageRequest pageRequest) {
    return toPage(users.findByTenantId(tenantId, pageRequest.pageable()));
  }

  public TchPage<UserRow> search(
      String nameLike, String status, Instant createdAfter, Instant createdBefore, TchPageRequest pageRequest) {
    return toPage(users.search(nameLike, status, createdAfter, createdBefore, pageRequest.pageable()));
  }

  private static TchPage<UserRow> toPage(Page<AppUser> page) {
    return TchPageMapper.map(
        page,
        user ->
            new UserRow(
                user.id(),
                user.keycloakSub(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.displayName(),
                user.status().name()));
  }

  private static String resolveUsername(String email, String phone) {
    if (email != null && !email.isBlank()) return email.trim();
    if (phone != null && !phone.isBlank()) return phone.trim();
    throw new IllegalArgumentException("Either email or phone must be provided");
  }

  private static String buildDisplayName(String firstName, String lastName) {
    return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
  }

  private static java.util.Locale parseLocale(String value) {
    return value == null || value.isBlank() ? null : java.util.Locale.forLanguageTag(value);
  }

  private static java.time.ZoneId parseZone(String value) {
    return value == null || value.isBlank() ? null : java.time.ZoneId.of(value);
  }

  private static java.util.Currency parseCurrency(String value) {
    return value == null || value.isBlank() ? null : java.util.Currency.getInstance(value);
  }

  public record UserProfile(
      UserId id,
      KeycloakUserSub keycloakSub,
      String username,
      String email,
      String phone,
      String status,
      String firstName,
      String lastName,
      String displayName) {}

  public record FirstLoginActivationResult(
      UserId userId,
      boolean mustChangePassword,
      boolean mustCompleteProfile,
      String entryRoute) {}
}
