package com.tchalanet.server.platform.identity.internal.firebase;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.platform.identity.api.ProvisionExternalUserRequest;
import com.tchalanet.server.platform.identity.api.ProvisionedExternalUser;
import com.tchalanet.server.platform.identity.api.SellerTerminalIdentityProvisioningApi;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression(
    "'${tch.identity.provider:firebase}' == 'firebase' || '${tch.identity.provider:firebase}' == 'firebase-emulator'")
@RequiredArgsConstructor
class FirebaseSellerTerminalIdentityProvisioningService
    implements SellerTerminalIdentityProvisioningApi {

    private final FirebaseUserProvisionService firebaseService;
    private final FirebaseIdentityProperties firebaseProperties;

    @Override
    public ProvisionedExternalUser provisionSellerTerminal(
        SellerTerminalId sellerTerminalId,
        String terminalCode,
        String displayName,
        String initialPin
    ) {
        var email = terminalCode.toLowerCase() + "@" + firebaseProperties.effectiveTerminalEmailDomain();
        return firebaseService.provisionUser(
            new ProvisionExternalUserRequest(
                sellerTerminalId.value().toString(),
                email,
                null,
                displayName,
                initialPin
            )
        );
    }

    @Override
    public void resetPasswordForSubject(String externalSubject, String newPassword) {
        firebaseService.resetPasswordForUid(externalSubject, newPassword);
    }
}
