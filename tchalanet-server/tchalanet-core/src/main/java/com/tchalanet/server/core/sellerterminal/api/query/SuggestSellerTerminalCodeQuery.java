package com.tchalanet.server.core.sellerterminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCodeSuggestionView;

public record SuggestSellerTerminalCodeQuery(TenantId tenantId)
    implements Query<SellerTerminalCodeSuggestionView> {}
