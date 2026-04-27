package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.core.user.domain.model.UserPreference;

public interface UserPreferenceWriterPort {
  UserPreference upsert(UserPreference pref);
}
