package com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.terminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal.SellerTerminal;

import java.util.Optional;

public interface SellerTerminalReaderPort {

    Optional<SellerTerminal> findById(TenantId tenantId, SellerTerminalId id);

    Optional<SellerTerminal> findByExternalSubject(String provider, String issuer, String externalSubject);

    TchPage<SellerTerminalSummaryRow> search(TenantId tenantId, SellerTerminalSearchCriteria criteria, TchPageRequest pageRequest);

    default SellerTerminal getRequired(TenantId tenantId, SellerTerminalId id) {
        return findById(tenantId, id)
            .orElseThrow(() -> new TchNotFoundException(id.toString(), "SellerTerminal not found: "));
    }
}
