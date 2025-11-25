package com.tchalanet.server.user.infra.persistence;

import com.tchalanet.server.user.domain.model.UserId;
import java.util.Locale;
import java.util.UUID;

/** Mapping simple entre entité JPA UserPreference et modèle domaine. */
class UserPreferenceEntityMapper {

  com.tchalanet.server.user.domain.model.UserPreference toDomain(UserPreferenceJpaEntity entity) {
    if (entity == null) return null;
    com.tchalanet.server.user.domain.model.UserPreference pref =
        new com.tchalanet.server.user.domain.model.UserPreference();
    if (entity.getUser() != null) {
      pref.setUserId(new UserId(entity.getUser().getId()));
    }
    pref.setThemeMode(entity.getThemeMode());
    pref.setDensity(entity.getDensity());
    if (entity.getLocale() != null) {
      pref.setLocale(Locale.forLanguageTag(entity.getLocale()));
    }
    return pref;
  }

  UserPreferenceJpaEntity toEntity(com.tchalanet.server.user.domain.model.UserPreference domain) {
    if (domain == null) return null;
    UserPreferenceJpaEntity entity = new UserPreferenceJpaEntity();
    UUID id = domain.getUserId().value();
    // set the user relation by creating a lightweight AppUserJpaEntity with the id
    AppUserJpaEntity user = new AppUserJpaEntity();
    user.setId(id);
    entity.setUser(user);
    entity.setThemeMode(domain.getThemeMode());
    entity.setDensity(domain.getDensity());
    if (domain.getLocale() != null) {
      entity.setLocale(domain.getLocale().toLanguageTag());
    }
    return entity;
  }
}
