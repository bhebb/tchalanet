package com.tchalanet.server.features.cashier.sellerterminal.identity;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.terminal.internal.infra.persistence.sellerterminal.SellerTerminalExternalIdentityJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.sellerterminal.SellerTerminalExternalIdentityJpaRepository;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.firebase.FirebaseIdentityProperties;
import com.tchalanet.server.platform.identity.internal.firebase.FirebaseUserProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression(
    "'${tch.identity.provider:firebase}' == 'firebase' || '${tch.identity.provider:firebase}' == 'firebase-emulator'")
@RequiredArgsConstructor
@Slf4j
class SellerTerminalIdentityProvisionAdapter implements SellerTerminalIdentityProvisionPort {

    private final FirebaseUserProvisionService firebaseService;
    private final FirebaseIdentityProperties firebaseProperties;
    private final SellerTerminalExternalIdentityJpaRepository identityRepo;

    @Override
    public void provision(
        SellerTerminalId id,
        TenantId tenantId,
        String terminalCode,
        String displayName,
        String initialPin
    ) {
        if (identityRepo.existsBySellerTerminalId(id.value())) {
            log.info("SellerTerminal {} already has external identity — skipping", id.value());
            return;
        }
        var email = terminalCode.toLowerCase() + "@" + firebaseProperties.effectiveTerminalEmailDomain();
        var result = firebaseService.provisionUser(
            id.value().toString(), email, null, displayName, initialPin);

        var entity = new SellerTerminalExternalIdentityJpaEntity();
        entity.setId(id.value());
        entity.setSellerTerminalId(id.value());
        entity.setProvider(IdentityProviderType.FIREBASE.name());
        entity.setIssuer(firebaseProperties.issuer());
        entity.setExternalSubject(result.uid());
        identityRepo.save(entity);

        log.info("Provisioned Firebase identity for SellerTerminal {} uid={}", id.value(), result.uid());
    }

    @Override
    public void resetPin(SellerTerminalId id, TenantId tenantId, String newPin) {
        var identity = identityRepo.findBySellerTerminalId(id.value())
            .orElseThrow(() -> new IllegalStateException(
                "No external identity found for SellerTerminal " + id.value()));
        firebaseService.resetPasswordForUid(identity.getExternalSubject(), newPin);
    }
}
