package com.tchalanet.server.core.external.port.out;

import com.tchalanet.server.common.types.id.KeycloakUserSub;
import java.util.Map;

public interface KeycloakUserProvisioningPort {
  Map<String, Object> createUser(Map<String, Object> payload);

  void resetPassword(String userId, String newPassword);

  void updateUserProfile(
      KeycloakUserSub keycloakSub, String firstName, String lastName, String email, String locale);

  void disableUser(KeycloakUserSub keycloakSub, String reason);
}
