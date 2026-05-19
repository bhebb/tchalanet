package com.tchalanet.server.core.limitpolicy.internal.application.query.handler.offline;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;
import com.tchalanet.server.core.limitpolicy.api.query.GetOfflineLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.offline.TenantOfflinePolicyReaderPort;
import com.tchalanet.server.core.limitpolicy.internal.infra.config.OfflineLimitPolicyProperties;
import lombok.RequiredArgsConstructor;

/**
 * Resolves the offline policy for a tenant:
 * <ol>
 *   <li>Look up the {@code tenant_offline_policy} row for this tenant.</li>
 *   <li>If none, fall back to the global defaults from
 *       {@link OfflineLimitPolicyProperties}.</li>
 * </ol>
 */
@UseCase
@RequiredArgsConstructor
public class GetOfflineLimitPolicyQueryHandler
    implements QueryHandler<GetOfflineLimitPolicyQuery, OfflineLimitPolicy> {

    private final TenantOfflinePolicyReaderPort tenantPolicyReader;
    private final OfflineLimitPolicyProperties rawProperties;

    @Override
    @TchTx(readOnly = true)
    public OfflineLimitPolicy handle(GetOfflineLimitPolicyQuery query) {
        return tenantPolicyReader.findByTenantId(query.tenantId())
            .orElseGet(this::globalDefault);
    }

    private OfflineLimitPolicy globalDefault() {
        var p = rawProperties.withDefaults();
        return new OfflineLimitPolicy(
            p.enabled(),
            p.batchSize(),
            p.validityDuration(),
            p.syncAcceptedExtension(),
            p.maxTicketCount(),
            new Money(p.maxTotalAmount(), CurrencyCode.of(p.currency()))
        );
    }
}
