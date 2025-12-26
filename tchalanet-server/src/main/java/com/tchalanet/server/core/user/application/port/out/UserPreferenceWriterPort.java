package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import java.time.Instant;

public interface UserPreferenceWriterPort {
  UserPreference save(UserPreference preference);

  void softDelete(UserId userId, Instant when);

  void upsert(UserId userId, String themeMode, Short density, String locale);
}
