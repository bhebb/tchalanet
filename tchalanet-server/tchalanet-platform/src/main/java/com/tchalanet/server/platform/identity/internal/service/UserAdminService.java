package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.identity.api.model.result.CreateUserResult;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.UserPreferenceJpaAdapter;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

  private final AppUserJpaAdapter users;
  private final UserPreferenceJpaAdapter preferences;
  private final TimeProvider timeProvider;

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

    var now = timeProvider.nowInstant();
    var user =
        users.save(
            AppUser.createNew(
                null,
                KeycloakUserSub.of(UUID.randomUUID()),
                resolveUsername(email, phone),
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
