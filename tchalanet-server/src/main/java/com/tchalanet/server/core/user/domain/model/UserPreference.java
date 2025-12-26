package com.tchalanet.server.core.user.domain.model;

import java.util.Locale;

import com.tchalanet.server.common.types.enums.ThemeMode;
import com.tchalanet.server.common.types.id.UserId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Préférences utilisateur côté domaine. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class UserPreference {

  private UserId userId;
  private ThemeMode themeMode; // nullable
  private Short density; // nullable
  private Locale locale; // nullable
}
