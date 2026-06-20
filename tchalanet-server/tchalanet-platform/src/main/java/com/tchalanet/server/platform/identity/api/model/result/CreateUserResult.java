package com.tchalanet.server.platform.identity.api.model.result;

import com.tchalanet.server.common.types.id.UserId;

public record CreateUserResult(
    UserId userId,
    boolean created,
    boolean temporaryCredentialIssued,
    String temporaryPassword) {

  public CreateUserResult(UserId userId) {
    this(userId, true, false, null);
  }
}
