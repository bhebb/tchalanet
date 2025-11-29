package com.tchalanet.server.core.user.domain.usecase;

import com.tchalanet.server.core.tenant.domain.model.TenantId;
import com.tchalanet.server.core.tenant.domain.usecase.GetPublishedThemeUseCase;
import com.tchalanet.server.core.user.domain.model.UserId;
import com.tchalanet.server.core.user.domain.ports.UserPreferenceRepository;
import com.tchalanet.server.core.user.web.dto.MeThemeResponse;
import com.tchalanet.server.core.user.web.dto.UserPrefDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use case pour récupérer le thème de l'utilisateur connecté. Combine le thème publié du tenant
 * avec les préférences utilisateur.
 */
@Service
@RequiredArgsConstructor
public class GetUserThemeUseCase {

  private final GetPublishedThemeUseCase getPublishedTheme;
  private final UserPreferenceRepository userPreferenceRepository;

  public MeThemeResponse execute(TenantId tenantId, UserId userId) {
    var themeRef = getPublishedTheme.execute(tenantId);
    var pref = userPreferenceRepository.findByUserId(userId).orElse(null);

    var user =
        (pref == null)
            ? new UserPrefDto(null, null, null)
            : new UserPrefDto(
                pref.getThemeMode(),
                pref.getDensity(),
                pref.getLocale() != null ? pref.getLocale().toString() : null);

    return new MeThemeResponse(themeRef, user);
  }

  /** Variante acceptant directement des UUIDs pour compatibilité */
  public MeThemeResponse execute(UUID tenantId, UUID userId) {
    return execute(new TenantId(tenantId), new UserId(userId));
  }
}
