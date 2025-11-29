package com.tchalanet.server.core.user.web;

import com.tchalanet.server.core.user.domain.usecase.UpdateUserPreferenceUseCaseImpl;
import com.tchalanet.server.core.user.web.dto.UpdateUserPreferenceRequest;
import com.tchalanet.server.core.user.web.dto.UserPreferenceResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** REST API pour la gestion des préférences utilisateur. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserPreferenceController {

  private final UpdateUserPreferenceUseCaseImpl updateUserPreferenceUseCase;

  @PutMapping("/{userId}/preferences")
  @PreAuthorize(
      "hasAuthority('USER_WRITE') or #userId.toString() == authentication.principal.subject")
  public ResponseEntity<UserPreferenceResponse> updatePreferences(
      @PathVariable UUID userId, @RequestBody UpdateUserPreferenceRequest request) {

    var updatedPref =
        updateUserPreferenceUseCase.updatePreferences(
            userId, request.themeMode(), request.density(), request.locale());

    UserPreferenceResponse response =
        new UserPreferenceResponse(
            userId,
            updatedPref.getThemeMode(),
            updatedPref.getDensity(),
            updatedPref.getLocale() != null ? updatedPref.getLocale().toString() : null);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{userId}/preferences")
  @PreAuthorize(
      "hasAuthority('USER_READ') or #userId.toString() == authentication.principal.subject")
  public ResponseEntity<UserPreferenceResponse> getPreferences(@PathVariable UUID userId) {
    // TODO: Implémenter avec le use case
    return ResponseEntity.ok(new UserPreferenceResponse(userId, null, null, null));
  }
}
