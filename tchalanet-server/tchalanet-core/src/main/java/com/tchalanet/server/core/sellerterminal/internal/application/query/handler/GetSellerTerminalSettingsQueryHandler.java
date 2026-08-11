package com.tchalanet.server.core.sellerterminal.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalNotificationSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalReceiptSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalSettingsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@UseCase
@RequiredArgsConstructor
public class GetSellerTerminalSettingsQueryHandler
    implements QueryHandler<GetSellerTerminalSettingsQuery, SellerTerminalSettingsView> {

  private final JdbcTemplate jdbc;

  @Override
  public SellerTerminalSettingsView handle(GetSellerTerminalSettingsQuery query) {
    try {
      return jdbc.queryForObject(
          """
          SELECT receipt_auto_print, receipt_copy_count, quick_sale,
                 receipt_printer_mode, receipt_paper_size, receipt_adapter_preference,
                 notifications_enabled, notifications_critical_only
            FROM seller_terminal_settings
           WHERE tenant_id = ? AND seller_terminal_id = ? AND deleted_at IS NULL
          """,
          (rs, rowNum) ->
              new SellerTerminalSettingsView(
                  new SellerTerminalReceiptSettingsView(
                      rs.getBoolean("receipt_auto_print"),
                      rs.getInt("receipt_copy_count"),
                      rs.getBoolean("quick_sale"),
                      rs.getString("receipt_printer_mode"),
                      rs.getString("receipt_paper_size"),
                      rs.getString("receipt_adapter_preference")),
                  new SellerTerminalNotificationSettingsView(
                      rs.getBoolean("notifications_enabled"),
                      rs.getBoolean("notifications_critical_only"))),
          query.tenantId().value(),
          query.sellerTerminalId().value());
    } catch (EmptyResultDataAccessException ignored) {
      return SellerTerminalSettingsView.defaults();
    }
  }
}
