package com.tchalanet.server.core.sellerterminal.internal.application.port.out;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;

import java.math.BigDecimal;
import java.util.Optional;

public interface SellerTerminalReaderPort {

    Optional<SellerTerminal> findById(TenantId tenantId, SellerTerminalId id);

    Optional<SellerTerminal> findByExternalSubject(String provider, String issuer, String externalSubject);

    TchPage<SellerTerminalSummaryRow> search(TenantId tenantId, SellerTerminalSearchCriteria criteria, TchPageRequest pageRequest);

    SellerTerminalCommissionStatsView commissionStats(TenantId tenantId, BigDecimal tenantDefaultRate);

    default SellerTerminal getRequired(TenantId tenantId, SellerTerminalId id) {
        return findById(tenantId, id)
            .orElseThrow(() -> new TchNotFoundException(id.toString(), "SellerTerminal not found: "));
    }
}
