package com.tchalanet.server.services;

import com.tchalanet.server.dto.PreferenceUpdateDto;
import com.tchalanet.server.model.UserPreference;
import com.tchalanet.server.repository.UserPreferenceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {
  private final UserPreferenceRepository repo;

  public UserPreference upsert(UUID userId, PreferenceUpdateDto dto) {
    var pref =
        repo.findById(userId)
            .orElseGet(
                () -> {
                  var p = new UserPreference();
                  p.setUserId(userId);
                  return p;
                });
    if (dto.themeMode() != null) pref.setThemeMode(dto.themeMode());
    if (dto.density() != null) pref.setDensity(dto.density());
    if (dto.locale() != null) pref.setLocale(dto.locale());
    return repo.save(pref);
  }
}
