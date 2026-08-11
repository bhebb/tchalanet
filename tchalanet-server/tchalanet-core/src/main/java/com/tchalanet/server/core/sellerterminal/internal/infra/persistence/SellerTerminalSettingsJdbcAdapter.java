package com.tchalanet.server.core.sellerterminal.internal.infra.persistence;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalNotificationSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalReceiptSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalSettingsReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalSettingsWriterPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SellerTerminalSettingsJdbcAdapter
    implements SellerTerminalSettingsReaderPort, SellerTerminalSettingsWriterPort {

  private final JdbcTemplate jdbc;

  @Override
  public SellerTerminalSettingsView get(TenantId tenantId, SellerTerminalId sellerTerminalId) {
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
          tenantId.value(),
          sellerTerminalId.value());
    } catch (EmptyResultDataAccessException ignored) {
      return defaults();
    }
  }

  @Override
  public void save(
      TenantId tenantId,
      SellerTerminalId sellerTerminalId,
      SellerTerminalSettingsView settings,
      UserId actorUserId) {
    var receipt = settings.receipt();
    var notifications = settings.notifications();
    jdbc.update(
        """
        INSERT INTO seller_terminal_settings (
          tenant_id, seller_terminal_id, receipt_auto_print, receipt_copy_count,
          quick_sale, receipt_printer_mode, receipt_paper_size, receipt_adapter_preference,
          notifications_enabled, notifications_critical_only, created_by, updated_by
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (tenant_id, seller_terminal_id)
        DO UPDATE SET
          receipt_auto_print = EXCLUDED.receipt_auto_print,
          receipt_copy_count = EXCLUDED.receipt_copy_count,
          quick_sale = EXCLUDED.quick_sale,
          receipt_printer_mode = EXCLUDED.receipt_printer_mode,
          receipt_paper_size = EXCLUDED.receipt_paper_size,
          receipt_adapter_preference = EXCLUDED.receipt_adapter_preference,
          notifications_enabled = EXCLUDED.notifications_enabled,
          notifications_critical_only = EXCLUDED.notifications_critical_only,
          updated_at = now(),
          updated_by = EXCLUDED.updated_by,
          version = seller_terminal_settings.version + 1
        """,
        tenantId.value(),
        sellerTerminalId.value(),
        receipt.autoPrint(),
        receipt.copyCount(),
        receipt.quickSale(),
        receipt.printerMode(),
        receipt.paperSize(),
        receipt.adapterPreference(),
        notifications.enabled(),
        notifications.criticalOnly(),
        actorUserId.value(),
        actorUserId.value());
  }

  private static SellerTerminalSettingsView defaults() {
    return new SellerTerminalSettingsView(
        new SellerTerminalReceiptSettingsView(true, 1, false, "AUTO", "RECEIPT_80MM", null),
        new SellerTerminalNotificationSettingsView(true, false));
  }
}
