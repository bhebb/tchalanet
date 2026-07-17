package com.tchalanet.server.features.tenantadmin.tickets;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.query.GetTicketPrintViewQuery;
import com.tchalanet.server.core.sales.api.settlement.SettlementExplanationApi;
import com.tchalanet.server.features.tenantadmin.tickets.model.AdminTicketSettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds the admin/support settlement-variant view for a ticket by recomputing each line's variant
 * via {@link SettlementExplanationApi}. A line whose bet option is unsupported (legacy/corrupted)
 * is reported as unresolved rather than failing the whole response.
 */
@Service
@RequiredArgsConstructor
public class AdminTicketSettlementService {

  private final QueryBus queryBus;
  private final SettlementExplanationApi settlementExplanation;

  public AdminTicketSettlementResponse getSettlementVariants(TicketId ticketId) {
    var view = queryBus.ask(new GetTicketPrintViewQuery(ticketId));

    var lines = view.lines().stream().map(this::toLineView).toList();

    return new AdminTicketSettlementResponse(ticketId.value().toString(), lines);
  }

  AdminTicketSettlementResponse.LineSettlementView toLineView(TicketPrintLine line) {
    try {
      var explained =
          settlementExplanation.explain(
              line.betType(), line.betOption(), line.selectionCanonical());

      return new AdminTicketSettlementResponse.LineSettlementView(
          line.lineNo(),
          line.gameCode().name(),
          line.betType().name(),
          line.betOption(),
          line.selectionCanonical(),
          line.selectionPolicySnapshot() == null ? null : line.selectionPolicySnapshot().name(),
          line.betOptionLabel(),
          line.settlementTermsSnapshot(),
          explained.commercialLabel(),
          explained.variant(),
          explained.adminLabel(),
          true,
          null);
    } catch (IllegalArgumentException ex) {
      return new AdminTicketSettlementResponse.LineSettlementView(
          line.lineNo(),
          line.gameCode().name(),
          line.betType().name(),
          line.betOption(),
          line.selectionCanonical(),
          line.selectionPolicySnapshot() == null ? null : line.selectionPolicySnapshot().name(),
          line.betOptionLabel(),
          line.settlementTermsSnapshot(),
          null,
          null,
          null,
          false,
          ex.getMessage());
    }
  }
}
