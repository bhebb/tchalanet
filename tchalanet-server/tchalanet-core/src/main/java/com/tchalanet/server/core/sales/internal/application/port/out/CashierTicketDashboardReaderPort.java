package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.query.CashierDashboardOverviewView;
import com.tchalanet.server.core.sales.api.query.CashierPendingApprovalView;
import com.tchalanet.server.core.sales.api.query.CashierRecentTicketView;
import com.tchalanet.server.core.sales.api.query.CashierTopSelectionsView;

import java.time.LocalDate;
import java.util.List;

public interface CashierTicketDashboardReaderPort {

    List<CashierRecentTicketView> findRecentByCashier(UserId cashierId, int limit);

    CashierDashboardOverviewView getOverview(TenantId tenantId, UserId cashierId, LocalDate businessDate);

    CashierTopSelectionsView findTopSelections(UserId cashierId, LocalDate businessDate, int limitPerDraw);

    List<CashierPendingApprovalView> findPendingApprovals(UserId cashierId, int limit);
}
