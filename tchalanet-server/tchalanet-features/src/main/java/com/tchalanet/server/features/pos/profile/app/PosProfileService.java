package com.tchalanet.server.features.pos.profile.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalCommercialCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalContactCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalLabelCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalSettingsCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalNotificationSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalReceiptSettingsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalSettingsQuery;
import com.tchalanet.server.features.pos.profile.model.PosProfileCommercialInfo;
import com.tchalanet.server.features.pos.profile.model.PosProfileDiagnosticsInfo;
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
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
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
  private final ClientDiagnosticsApi clientDiagnosticsApi;
  private final TenantConfigApi tenantConfigApi;

  public PosProfileResponse profile(TchRequestContext ctx) {
    var sellerTerminalId = ctx.sellerTerminalIdRequired();
    var tenantId = ctx.effectiveTenantIdRequired();
    var terminal = queryBus.ask(new GetSellerTerminalQuery(tenantId, sellerTerminalId));
    var currency = ctx.tenantCurrency() == null ? null : ctx.tenantCurrency();
    var locale = ctx.locale() == null ? Locale.FRENCH : ctx.locale();
    var zone = ctx.tenantZoneId();
    var tenant = tenantConfigApi.getTenantById(new GetTenantByIdRequest(tenantId));

    var displayName =
        terminal.displayName() == null || terminal.displayName().isBlank()
            ? terminal.terminalCode()
            : terminal.displayName();
    var ready = terminal.status() == SellerTerminalStatus.ACTIVE && !terminal.mustChangePin();
    var settings = queryBus.ask(new GetSellerTerminalSettingsQuery(tenantId, sellerTerminalId));

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
            tenantDisplayName(tenant.displayName(), tenant.name(), ctx.effectiveTenantCode()),
            currencyCode(currency),
            terminal.commissionRate()),
        new PosProfileSettingsInfo(
            locale.toLanguageTag(),
            zone == null ? null : zone.getId(),
            currencyCode(currency),
            SUPPORTED_LOCALES,
            toReceiptSettings(settings.receipt()),
            toNotificationSettings(settings.notifications())),
        toDiagnostics(clientDiagnosticsApi.getPolicy(tenantId, sellerTerminalId)));
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
    var tenantId = ctx.tenantIdRequired();
    var terminalId = ctx.sellerTerminalIdRequired();
    var current = queryBus.ask(new GetSellerTerminalSettingsQuery(tenantId, terminalId));
    var receipt =
        new SellerTerminalReceiptSettingsView(
            request.receiptAutoPrint() == null
                ? current.receipt().autoPrint()
                : request.receiptAutoPrint(),
            request.receiptCopyCount() == null
                ? current.receipt().copyCount()
                : request.receiptCopyCount(),
            request.receiptQuickSale() == null
                ? current.receipt().quickSale()
                : request.receiptQuickSale(),
            request.receiptPrinterMode() == null
                ? current.receipt().printerMode()
                : request.receiptPrinterMode(),
            request.receiptPaperSize() == null
                ? current.receipt().paperSize()
                : request.receiptPaperSize(),
            request.receiptAdapterPreference() == null
                ? current.receipt().adapterPreference()
                : request.receiptAdapterPreference());
    var notifications =
        new SellerTerminalNotificationSettingsView(
            request.notificationsEnabled() == null
                ? current.notifications().enabled()
                : request.notificationsEnabled(),
            request.notificationsCriticalOnly() == null
                ? current.notifications().criticalOnly()
                : request.notificationsCriticalOnly());

    commandBus.execute(
        new UpdateSellerTerminalSettingsCommand(
            tenantId, terminalId, receipt, notifications, ctx.userId()));
    return profile(ctx);
  }

  private static PosProfileReceiptSettings toReceiptSettings(
      SellerTerminalReceiptSettingsView settings) {
    return new PosProfileReceiptSettings(
        settings.autoPrint(),
        settings.copyCount(),
        settings.quickSale(),
        settings.printerMode(),
        settings.paperSize(),
        settings.adapterPreference());
  }

  private static PosProfileNotificationSettings toNotificationSettings(
      SellerTerminalNotificationSettingsView settings) {
    return new PosProfileNotificationSettings(settings.enabled(), settings.criticalOnly());
  }

  private static PosProfileDiagnosticsInfo toDiagnostics(
      com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView policy) {
    return new PosProfileDiagnosticsInfo(
        policy.enabled(), policy.expiresAt(), policy.maxEvents(), policy.categories());
  }

  private static String sellerDisplayName(String firstName, String lastName, String fallback) {
    var fullName =
        ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    return fullName.isBlank() ? fallback : fullName;
  }

  private static String tenantDisplayName(String displayName, String name, String fallback) {
    if (displayName != null && !displayName.isBlank()) {
      return displayName;
    }
    if (name != null && !name.isBlank()) {
      return name;
    }
    return fallback;
  }

  private static String currencyCode(Currency currency) {
    return currency == null ? null : currency.getCurrencyCode();
  }
}
