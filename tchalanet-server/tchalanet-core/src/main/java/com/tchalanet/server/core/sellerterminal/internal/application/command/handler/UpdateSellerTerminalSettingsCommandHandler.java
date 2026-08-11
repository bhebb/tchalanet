package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalSettingsCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalNotificationSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalReceiptSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalSettingsReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalSettingsWriterPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateSellerTerminalSettingsCommandHandler
    implements CommandHandler<UpdateSellerTerminalSettingsCommand, Void> {

  private final SellerTerminalSettingsReaderPort reader;
  private final SellerTerminalSettingsWriterPort writer;

  @Override
  @TchTx
  public Void handle(UpdateSellerTerminalSettingsCommand command) {
    var current = reader.get(command.tenantId(), command.sellerTerminalId());
    var currentReceipt = current.receipt();
    var currentNotifications = current.notifications();
    var receipt =
        new SellerTerminalReceiptSettingsView(
            valueOr(command.receiptAutoPrint(), currentReceipt.autoPrint()),
            valueOr(command.receiptCopyCount(), currentReceipt.copyCount()),
            valueOr(command.receiptQuickSale(), currentReceipt.quickSale()),
            valueOr(command.receiptPrinterMode(), currentReceipt.printerMode()),
            valueOr(command.receiptPaperSize(), currentReceipt.paperSize()),
            command.receiptAdapterPreference() == null
                ? currentReceipt.adapterPreference()
                : command.receiptAdapterPreference());
    validate(receipt);
    var notifications =
        new SellerTerminalNotificationSettingsView(
            valueOr(command.notificationsEnabled(), currentNotifications.enabled()),
            valueOr(
                command.notificationsCriticalOnly(), currentNotifications.criticalOnly()));
    writer.save(
        command.tenantId(),
        command.sellerTerminalId(),
        new SellerTerminalSettingsView(receipt, notifications),
        command.actorUserId());
    return null;
  }

  private static <T> T valueOr(T value, T fallback) {
    return value == null ? fallback : value;
  }

  private static void validate(SellerTerminalReceiptSettingsView receipt) {
    if (receipt.copyCount() < 1 || receipt.copyCount() > 3) {
      throw new IllegalArgumentException("Receipt copy count must be between 1 and 3");
    }
    if (!List.of("AUTO", "POS_DIRECT", "SYSTEM_PDF").contains(receipt.printerMode())) {
      throw new IllegalArgumentException("Unsupported receipt printer mode");
    }
    if (!List.of("RECEIPT_58MM", "RECEIPT_80MM").contains(receipt.paperSize())) {
      throw new IllegalArgumentException("Unsupported receipt paper size");
    }
  }
}
