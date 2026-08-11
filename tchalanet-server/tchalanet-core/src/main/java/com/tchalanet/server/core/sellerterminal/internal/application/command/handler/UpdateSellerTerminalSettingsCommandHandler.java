package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalSettingsCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@UseCase
@RequiredArgsConstructor
public class UpdateSellerTerminalSettingsCommandHandler
    implements CommandHandler<UpdateSellerTerminalSettingsCommand, Void> {

  private static final List<String> PRINTER_MODES = List.of("AUTO", "POS_DIRECT", "SYSTEM_PDF");
  private static final List<String> PAPER_SIZES = List.of("RECEIPT_58MM", "RECEIPT_80MM");

  private final JdbcTemplate jdbc;

  @Override
  @TchTx
  public Void handle(UpdateSellerTerminalSettingsCommand command) {
    validate(command);
    var actorUserId = command.actorUserId() == null ? null : command.actorUserId().value();
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
        command.tenantId().value(),
        command.sellerTerminalId().value(),
        command.receipt().autoPrint(),
        command.receipt().copyCount(),
        command.receipt().quickSale(),
        command.receipt().printerMode(),
        command.receipt().paperSize(),
        command.receipt().adapterPreference(),
        command.notifications().enabled(),
        command.notifications().criticalOnly(),
        actorUserId,
        actorUserId);
    return null;
  }

  private static void validate(UpdateSellerTerminalSettingsCommand command) {
    if (!PRINTER_MODES.contains(command.receipt().printerMode())) {
      throw new IllegalArgumentException("Unsupported receipt printer mode");
    }
    if (!PAPER_SIZES.contains(command.receipt().paperSize())) {
      throw new IllegalArgumentException("Unsupported receipt paper size");
    }
  }
}
