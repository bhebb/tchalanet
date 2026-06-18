package com.tchalanet.server.core.sellerterminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;

public record ListSellerTerminalsQuery(
    TenantId tenantId,
    SellerTerminalSearchCriteria criteria,
    TchPageRequest pageRequest
) implements Query<TchPage<SellerTerminalSummaryRow>> {}
