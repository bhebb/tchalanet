package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import java.util.Optional;

public interface UserPreferenceReaderPort {
  Optional<UserPreference> findByUserId(UserId userId);
}
