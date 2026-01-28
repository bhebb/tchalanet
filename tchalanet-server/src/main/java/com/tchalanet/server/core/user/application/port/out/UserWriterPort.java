package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.common.types.enums.UserStatus;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.domain.model.AppUser;
import java.time.Instant;

public interface UserWriterPort {
  AppUser save(AppUser user);

  void softDelete(UserId userId, Instant when);

  void updateStatus(UserId userId, UserStatus userStatus);
}
