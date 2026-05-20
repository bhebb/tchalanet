package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.view.TicketDetailsView;
import com.tchalanet.server.core.sales.api.model.view.TicketForDrawSettlementView;
import com.tchalanet.server.core.sales.api.model.view.TicketForPayoutView;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;

import java.util.List;
import java.util.Optional;

public interface TicketProjectionReaderPort {
    TicketDetailsView getDetailsById(TicketId ticketId);

    TicketPrintView getPrintViewById(TicketId ticketId);

    TicketForPayoutView getForPayoutById(TicketId ticketId);

    List<TicketForDrawSettlementView> findForDrawSettlement(DrawId drawId);

    TchPage<TicketRow> list(ListTicketsQuery query);

    Optional<TicketPrintView> findPrintView(TicketId ticketId);
}
