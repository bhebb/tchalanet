package com.tchalanet.server.platform.identity.api;

import com.tchalanet.server.common.types.id.SellerTerminalId;

public interface SellerTerminalIdentityProvisioningApi {

    ProvisionedExternalUser provisionSellerTerminal(
        SellerTerminalId sellerTerminalId,
        String terminalCode,
        String displayName,
        String initialPin
    );

    void resetPasswordForSubject(String externalSubject, String newPassword);
}
