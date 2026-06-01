package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import com.tchalanet.server.platform.identity.internal.model.UserPreference;
import com.tchalanet.server.platform.identity.internal.model.UserRow;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.UserPreferenceJpaAdapter;
import com.tchalanet.server.platform.identity.internal.service.keycloak.KeycloakUserProvisionService;
import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByIdRequest;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantUserAdministrationService {

  private final AppUserJpaAdapter users;
  private final UserPreferenceJpaAdapter preferences;
  private final TimeProvider timeProvider;
  private final KeycloakUserProvisionService keycloakUserProvisionService;
  private final TenantConfigApi tenantConfigApi;

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
    var tenantCode = resolveTenantCodeOrNull();

    // Provision the identity in Keycloak so the new user can actually authenticate. Roles are
    // resolved app-side (tenant_user_role), so no realm role is assigned here — the user only
    // needs username/password + the tenant_code attribute (mapped to the token claim). When KC
    // bootstrap is disabled this returns an empty result and we fall back to a random sub
    // (offline/unit-test behaviour). The returned KC id becomes app_user.keycloak_sub so the
    // JWT 'sub' on first login resolves to this row.
    var kc =
        keycloakUserProvisionService.provisionUser(
            username, email, firstName, lastName, tenantCode, "pro", null);
    var keycloakSub =
        kc.keycloakId() != null
            ? KeycloakUserSub.of(kc.keycloakId())
            : KeycloakUserSub.of(UUID.randomUUID());

    var now = timeProvider.nowInstant();
    var user =
        users.save(
            AppUser.createNew(
                null,
                keycloakSub,
                username,
                email,
                phone,
                firstName,
                lastName,
                buildDisplayName(firstName, lastName),
                null,
                now));
    preferences.upsert(UserPreference.forUser(user.id()).applyOverrides(prefThemeMode, prefDensity, parseLocale(prefLocale), parseZone(prefTimeZone), parseCurrency(prefCurrency)));
    return new CreateUserResult(user.id());
  }

  /**
   * Creates a user with an explicitly provided tenantCode for KC provisioning.
   * Use this from provisioning orchestrators that have the tenant code at hand —
   * avoids the context-lookup that fails inside startup-tenant scopes.
   */
  @Transactional
  public CreateUserResult createUserForTenant(
      String email,
      String phone,
      String firstName,
      String lastName,
      String tenantCode) {
    if (users.findByEmailOrPhone(email, phone).isPresent()) {
      throw new IllegalStateException("User already exists with this email or phone");
    }
    var username = resolveUsername(email, phone);
    var kc = keycloakUserProvisionService.provisionUser(
        username, email, firstName, lastName, tenantCode, "pro", null);
    var keycloakSub = kc.keycloakId() != null
        ? KeycloakUserSub.of(kc.keycloakId())
        : KeycloakUserSub.of(UUID.randomUUID());
    var now = timeProvider.nowInstant();
    var user = users.save(AppUser.createNew(
        null, keycloakSub, username, email, phone,
        firstName, lastName, buildDisplayName(firstName, lastName), null, now));
    return new CreateUserResult(user.id());
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

  public void resyncKeycloak(UserId userId) {
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

  /**
   * Resolves the current request's tenant code (for the Keycloak tenant_code attribute) from the
   * effective tenant in {@link TchContext}. Returns null for platform-scoped creation or when it
   * can't be resolved — the user is still created, just without a tenant claim.
   */
  private String resolveTenantCodeOrNull() {
    var ctx = TchContext.currentOrNull();
    if (ctx == null) {
      return null;
    }
    var tenantId = ctx.effectiveTenantIdOrNull();
    if (tenantId == null) {
      return null;
    }
    try {
      var view = tenantConfigApi.getTenantById(new GetTenantByIdRequest(tenantId));
      return view != null ? view.code() : null;
    } catch (RuntimeException e) {
      log.warn("Could not resolve tenant code for tenantId={} during user creation: {}",
          tenantId, e.getMessage());
      return null;
    }
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
}
