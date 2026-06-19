package com.tchalanet.server.core.sellerterminal.internal.infra.identity;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.infra.persistence.SellerTerminalExternalIdentityJpaEntity;
import com.tchalanet.server.core.sellerterminal.internal.infra.persistence.SellerTerminalExternalIdentityJpaRepository;
import com.tchalanet.server.platform.identity.api.SellerTerminalIdentityProvisioningApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class CoreSellerTerminalIdentityProvisionAdapter implements SellerTerminalIdentityProvisionPort {

    private final SellerTerminalIdentityProvisioningApi identityProvisioning;
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
            log.info("SellerTerminal {} already has external identity - skipping", id.value());
            return;
        }
        var result = identityProvisioning.provisionSellerTerminal(
            id, terminalCode, displayName, initialPin);

        var entity = new SellerTerminalExternalIdentityJpaEntity();
        entity.setId(id.value());
        entity.setSellerTerminalId(id.value());
        entity.setProvider(result.provider().name());
        entity.setIssuer(result.issuer());
        entity.setExternalSubject(result.externalSubject());
        identityRepo.save(entity);

        log.info(
            "Provisioned external identity for SellerTerminal {} subject={}",
            id.value(),
            result.externalSubject());
    }

    @Override
    public void resetPin(SellerTerminalId id, TenantId tenantId, String newPin) {
        var identity = identityRepo.findBySellerTerminalId(id.value())
            .orElseThrow(() -> new IllegalStateException(
                "No external identity found for SellerTerminal " + id.value()));
        identityProvisioning.resetPasswordForSubject(identity.getExternalSubject(), newPin);
    }
}
