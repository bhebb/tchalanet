package com.tchalanet.server.features.cashier.sellerterminal.identity;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.terminal.internal.infra.persistence.sellerterminal.SellerTerminalJpaRepository;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.persistence.SellerTerminalExternalIdentityPort;
import com.tchalanet.server.platform.identity.internal.service.SellerTerminalIdentityResolution;
import com.tchalanet.server.platform.identity.internal.service.SellerTerminalIdentityResolution.TerminalBootstrapStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class SellerTerminalExternalIdentityAdapter implements SellerTerminalExternalIdentityPort {

    private final SellerTerminalJpaRepository terminalRepo;

    @Override
    public Optional<SellerTerminalIdentityResolution> findByExternalIdentity(
        IdentityProviderType provider,
        String issuer,
        String externalSubject
    ) {
        return terminalRepo
            .findByExternalSubject(provider.name(), issuer, externalSubject)
            .map(entity -> new SellerTerminalIdentityResolution(
                SellerTerminalId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                toBootstrapStatus(entity.getStatus())
            ));
    }

    private TerminalBootstrapStatus toBootstrapStatus(SellerTerminalStatus status) {
        return switch (status) {
            case ACTIVE -> TerminalBootstrapStatus.ACTIVE;
            case BLOCKED -> TerminalBootstrapStatus.BLOCKED;
            default -> TerminalBootstrapStatus.DISABLED;
        };
    }
}
