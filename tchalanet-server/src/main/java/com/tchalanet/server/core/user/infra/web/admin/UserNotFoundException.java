package com.tchalanet.server.core.user.infra.web.admin;

public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(Object id) {
    super("User not found: " + id);
  }
}
