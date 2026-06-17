package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.platform.identity.api.IdentityProvisioningApi;
import com.tchalanet.server.platform.identity.api.ProvisionExternalUserRequest;
import com.tchalanet.server.platform.identity.api.ProvisionedExternalUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression(
    "'${tch.identity.provider:firebase}' != 'firebase' && '${tch.identity.provider:firebase}' != 'firebase-emulator'")
public class UnsupportedIdentityProvisioningService implements IdentityProvisioningApi {

  private final String configuredProvider;

  public UnsupportedIdentityProvisioningService(
      @Value("${tch.identity.provider:firebase}") String configuredProvider) {
    this.configuredProvider = configuredProvider;
  }

  @Override
  public ProvisionedExternalUser provisionUser(ProvisionExternalUserRequest request) {
    throw new IllegalStateException(
        "Managed identity provisioning is not supported for configured provider: "
            + configuredProvider);
  }
}

