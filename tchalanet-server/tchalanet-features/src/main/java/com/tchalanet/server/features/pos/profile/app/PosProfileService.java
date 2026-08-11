package com.tchalanet.server.features.pos.profile.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalSettingsCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalCommercialCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalContactCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalLabelCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSettingsView;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalSettingsQuery;
import com.tchalanet.server.features.pos.profile.model.PosProfileCommercialInfo;
import com.tchalanet.server.features.pos.profile.model.PosProfileNotificationSettings;
import com.tchalanet.server.features.pos.profile.model.PosProfileReceiptSettings;
import com.tchalanet.server.features.pos.profile.model.PosProfileResponse;
import com.tchalanet.server.features.pos.profile.model.PosProfileSellerInfo;
import com.tchalanet.server.features.pos.profile.model.PosProfileSettingsInfo;
import com.tchalanet.server.features.pos.profile.model.PosProfileTerminalInfo;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileCommercialRequest;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileSellerRequest;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileSettingsRequest;
import com.tchalanet.server.features.pos.profile.model.UpdatePosProfileTerminalRequest;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PosProfileService {

  private static final String VERSION = "profile.v1";
  private static final List<String> SUPPORTED_LOCALES = List.of("ht", "fr", "en");

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public PosProfileResponse profile(TchRequestContext ctx) {
    var sellerTerminalId = ctx.sellerTerminalIdRequired();
    var tenantId = ctx.effectiveTenantIdRequired();
    var terminal = queryBus.ask(new GetSellerTerminalQuery(tenantId, sellerTerminalId));
    var currency = ctx.tenantCurrency() == null ? null : ctx.tenantCurrency();
    var locale = ctx.locale() == null ? Locale.FRENCH : ctx.locale();
    var zone = ctx.tenantZoneId();

    var displayName =
        terminal.displayName() == null || terminal.displayName().isBlank()
            ? terminal.terminalCode()
            : terminal.displayName();
    var ready = terminal.status() == SellerTerminalStatus.ACTIVE && !terminal.mustChangePin();
    var settings =
        queryBus.ask(new GetSellerTerminalSettingsQuery(tenantId, sellerTerminalId));

    return new PosProfileResponse(
        VERSION,
        new PosProfileTerminalInfo(
            sellerTerminalId.value().toString(),
            terminal.terminalCode(),
            displayName,
            terminal.status().name(),
            ready,
            true,
            terminal.mustChangePin(),
            terminal.lastSeenAt(),
            "SELLER_TERMINAL"),
        new PosProfileSellerInfo(
            terminal.firstName(),
            terminal.lastName(),
            sellerDisplayName(terminal.firstName(), terminal.lastName(), displayName),
            terminal.email(),
            terminal.phoneNumber(),
            terminal.addressId() == null ? null : terminal.addressId().value().toString()),
        new PosProfileCommercialInfo(
            tenantId.value().toString(),
            ctx.effectiveTenantCode(),
            currencyCode(currency),
            terminal.commissionRate()),
        new PosProfileSettingsInfo(
            locale.toLanguageTag(),
            zone == null ? null : zone.getId(),
            currencyCode(currency),
            SUPPORTED_LOCALES,
            toReceiptSettings(settings),
            toNotificationSettings(settings)));
  }

  public PosProfileResponse updateTerminal(
      TchRequestContext ctx, UpdatePosProfileTerminalRequest request) {
    commandBus.execute(
        new UpdateSellerTerminalLabelCommand(
            ctx.tenantIdRequired(),
            ctx.sellerTerminalIdRequired(),
            request.displayName(),
            ctx.userId()));
    return profile(ctx);
  }

  public PosProfileResponse updateSeller(
      TchRequestContext ctx, UpdatePosProfileSellerRequest request) {
    commandBus.execute(
        new UpdateSellerTerminalContactCommand(
            ctx.tenantIdRequired(),
            ctx.sellerTerminalIdRequired(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phoneNumber(),
            request.addressId(),
            ctx.userId()));
    return profile(ctx);
  }

  public PosProfileResponse updateCommercial(
      TchRequestContext ctx, UpdatePosProfileCommercialRequest request) {
    commandBus.execute(
        new UpdateSellerTerminalCommercialCommand(
            ctx.tenantIdRequired(),
            ctx.sellerTerminalIdRequired(),
            request.commissionRate(),
            ctx.userId()));
    return profile(ctx);
  }

  public PosProfileResponse updateSettings(
      TchRequestContext ctx, UpdatePosProfileSettingsRequest request) {
    commandBus.execute(
        new UpdateSellerTerminalSettingsCommand(
            ctx.tenantIdRequired(),
            ctx.sellerTerminalIdRequired(),
            request.receiptAutoPrint(),
            request.receiptCopyCount(),
            request.receiptQuickSale(),
            request.receiptPrinterMode(),
            request.receiptPaperSize(),
            request.receiptAdapterPreference(),
            request.notificationsEnabled(),
            request.notificationsCriticalOnly(),
            ctx.userId()));
    return profile(ctx);
  }

  private static PosProfileReceiptSettings toReceiptSettings(SellerTerminalSettingsView settings) {
    var receipt = settings.receipt();
    return new PosProfileReceiptSettings(
        receipt.autoPrint(),
        receipt.copyCount(),
        receipt.quickSale(),
        receipt.printerMode(),
        receipt.paperSize(),
        receipt.adapterPreference());
  }

  private static PosProfileNotificationSettings toNotificationSettings(
      SellerTerminalSettingsView settings) {
    var notifications = settings.notifications();
    return new PosProfileNotificationSettings(notifications.enabled(), notifications.criticalOnly());
  }

  private static String sellerDisplayName(String firstName, String lastName, String fallback) {
    var fullName =
        ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    return fullName.isBlank() ? fallback : fullName;
  }

  private static String currencyCode(Currency currency) {
    return currency == null ? null : currency.getCurrencyCode();
  }
}
