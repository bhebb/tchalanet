package com.tchalanet.server.core.sales.application.command.model.offline;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.math.BigDecimal;
import java.time.Instant;

public record OfflineTicketSaleInput(
    OfflineSaleSubmissionId submissionId,
    String offlineCode,
    OutletId outletId,
    TerminalId terminalId,
    UserId sellerUserId,
    SalesSessionId salesSessionId,
    DrawId drawId,
    Instant createdAtDevice,
    BigDecimal stakeAmount,
    BigDecimal totalAmount
) {}

