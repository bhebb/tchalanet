package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.platform.identity.api.ProvisionedExternalUser;
import com.tchalanet.server.platform.identity.api.SellerTerminalIdentityProvisioningApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression(
    "'${tch.identity.provider:firebase}' != 'firebase' && '${tch.identity.provider:firebase}' != 'firebase-emulator'")
class UnsupportedSellerTerminalIdentityProvisioningService
    implements SellerTerminalIdentityProvisioningApi {

    private final String configuredProvider;

    UnsupportedSellerTerminalIdentityProvisioningService(
        @Value("${tch.identity.provider:firebase}") String configuredProvider
    ) {
        this.configuredProvider = configuredProvider;
    }

    @Override
    public ProvisionedExternalUser provisionSellerTerminal(
        SellerTerminalId sellerTerminalId,
        String terminalCode,
        String displayName,
        String initialPin
    ) {
        throw unsupported();
    }

    @Override
    public void resetPasswordForSubject(String externalSubject, String newPassword) {
        throw unsupported();
    }

    private IllegalStateException unsupported() {
        return new IllegalStateException(
            "SellerTerminal identity provisioning is not supported for configured provider: "
                + configuredProvider);
    }
}
