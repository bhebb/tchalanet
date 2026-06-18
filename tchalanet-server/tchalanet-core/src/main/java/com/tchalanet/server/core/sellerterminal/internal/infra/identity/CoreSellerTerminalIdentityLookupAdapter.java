package com.tchalanet.server.core.sellerterminal.internal.infra.identity;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.internal.infra.persistence.SellerTerminalJpaRepository;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.SellerTerminalIdentityLookup;
import com.tchalanet.server.platform.identity.api.model.SellerTerminalBootstrapStatus;
import com.tchalanet.server.platform.identity.api.model.SellerTerminalIdentityBootstrapView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class CoreSellerTerminalIdentityLookupAdapter implements SellerTerminalIdentityLookup {

    private final SellerTerminalJpaRepository terminalRepo;

    @Override
    public Optional<SellerTerminalIdentityBootstrapView> findByExternalIdentity(
        IdentityProviderType provider,
        String issuer,
        String externalSubject
    ) {
        return terminalRepo
            .findByExternalSubject(provider.name(), issuer, externalSubject)
            .map(entity -> new SellerTerminalIdentityBootstrapView(
                SellerTerminalId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                toBootstrapStatus(entity.getStatus())
            ));
    }

    private SellerTerminalBootstrapStatus toBootstrapStatus(SellerTerminalStatus status) {
        return switch (status) {
            case ACTIVE -> SellerTerminalBootstrapStatus.ACTIVE;
            case BLOCKED -> SellerTerminalBootstrapStatus.BLOCKED;
            case DISABLED -> SellerTerminalBootstrapStatus.DISABLED;
            case PENDING -> SellerTerminalBootstrapStatus.SUSPENDED;
        };
    }
}
