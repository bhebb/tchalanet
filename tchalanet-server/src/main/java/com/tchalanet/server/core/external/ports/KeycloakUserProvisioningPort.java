package com.tchalanet.server.core.external.ports;

import java.util.Map;

public interface KeycloakUserProvisioningPort {
  Map<String, Object> createUser(Map<String, Object> payload);

  void resetPassword(String userId, String newPassword);
}
