package com.tchalanet.server.core.promotion.internal.infra.web.admin.request;

import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SimulatePromotionRequest(
    @NotNull PromotionEvaluationPhase phase,
    Instant at,

    AgentId agentId,
    List<AgentId> agentPath,
    AgentZoneId zoneId,
    List<AgentZoneId> zonePath,

    OutletId outletId,
    TerminalId terminalId,
    SalesSessionId salesSessionId,
    UserId sellerUserId,

    @NotNull BigDecimal paidTotal,
    @NotNull String currency,
    List<String> paidGameCodes,
    Integer paidLineCount,
    boolean offline
) {}

