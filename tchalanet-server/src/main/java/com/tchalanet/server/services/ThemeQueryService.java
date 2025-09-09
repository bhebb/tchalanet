package com.tchalanet.server.services;

import com.tchalanet.server.constants.ThemeMode;
import com.tchalanet.server.constants.ThemeStatus;
import com.tchalanet.server.dto.MeThemeResponse;
import com.tchalanet.server.dto.ThemeRefDto;
import com.tchalanet.server.dto.UserPrefDto;
import com.tchalanet.server.repository.ThemeRepository;
import com.tchalanet.server.repository.UserPreferenceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ThemeQueryService {
  private final ThemeRepository themes;
  private final UserPreferenceRepository prefs;

  @Cacheable(value = "publishedThemeByTenant", key = "#tenantId", unless = "#result == null")
  public ThemeRefDto getPublishedThemeRef(UUID tenantId) {
    return themes
        .findFirstByTenantIdAndStatusOrderByUpdatedAtDesc(tenantId, ThemeStatus.PUBLISHED)
        .map(t -> new ThemeRefDto(t.getId().toString(), t.getMode(), t.getDensity()))
        .orElseGet(() -> new ThemeRefDto("tchalanet", ThemeMode.SYSTEM, (short) 0));
  }

  public MeThemeResponse getUserTheme(UUID tenantId, UUID userId) {
    var themeRef = getPublishedThemeRef(tenantId);
    var pref = prefs.findById(userId).orElse(null);
    var user =
        (pref == null)
            ? new UserPrefDto(null, null, null)
            : new UserPrefDto(pref.getThemeMode(), pref.getDensity(), pref.getLocale());
    return new MeThemeResponse(themeRef, user);
  }

  /** Invalidation cache après publish/update */
  @CacheEvict(value = "publishedThemeByTenant", key = "#tenantId")
  public void evictTenantTheme(UUID tenantId) {
    /* no-op */
  }
}
