package com.tchalanet.server.features.pos.profile.app;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalCommercialCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalContactCommand;
import com.tchalanet.server.core.sellerterminal.api.command.UpdateSellerTerminalLabelCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PosProfileService {

  private static final String VERSION = "profile.v1";
  private static final List<String> SUPPORTED_LOCALES = List.of("ht", "fr", "en");

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final JdbcTemplate jdbc;

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
    var settings = settings(tenantId.value(), sellerTerminalId.value());

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
            settings.receipt(),
            settings.notifications()));
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
    var tenantId = ctx.tenantIdRequired().value();
    var terminalId = ctx.sellerTerminalIdRequired().value();
    var current = settings(tenantId, terminalId);
    var receipt =
        new PosProfileReceiptSettings(
            request.receiptAutoPrint() == null
                ? current.receipt().autoPrint()
                : request.receiptAutoPrint(),
            request.receiptCopyCount() == null
                ? current.receipt().copyCount()
                : request.receiptCopyCount());
    var notifications =
        new PosProfileNotificationSettings(
            request.notificationsEnabled() == null
                ? current.notifications().enabled()
                : request.notificationsEnabled(),
            request.notificationsCriticalOnly() == null
                ? current.notifications().criticalOnly()
                : request.notificationsCriticalOnly());

    jdbc.update(
        """
        INSERT INTO seller_terminal_settings (
          tenant_id, seller_terminal_id, receipt_auto_print, receipt_copy_count,
          notifications_enabled, notifications_critical_only, created_by, updated_by
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (tenant_id, seller_terminal_id)
        DO UPDATE SET
          receipt_auto_print = EXCLUDED.receipt_auto_print,
          receipt_copy_count = EXCLUDED.receipt_copy_count,
          notifications_enabled = EXCLUDED.notifications_enabled,
          notifications_critical_only = EXCLUDED.notifications_critical_only,
          updated_at = now(),
          updated_by = EXCLUDED.updated_by,
          version = seller_terminal_settings.version + 1
        """,
        tenantId,
        terminalId,
        receipt.autoPrint(),
        receipt.copyCount(),
        notifications.enabled(),
        notifications.criticalOnly(),
        ctx.userUuid(),
        ctx.userUuid());
    return profile(ctx);
  }

  private PosSettings settings(UUID tenantId, UUID terminalId) {
    try {
      return jdbc.queryForObject(
          """
          SELECT receipt_auto_print, receipt_copy_count,
                 notifications_enabled, notifications_critical_only
            FROM seller_terminal_settings
           WHERE tenant_id = ? AND seller_terminal_id = ? AND deleted_at IS NULL
          """,
          (rs, rowNum) ->
              new PosSettings(
                  new PosProfileReceiptSettings(
                      rs.getBoolean("receipt_auto_print"), rs.getInt("receipt_copy_count")),
                  new PosProfileNotificationSettings(
                      rs.getBoolean("notifications_enabled"),
                      rs.getBoolean("notifications_critical_only"))),
          tenantId,
          terminalId);
    } catch (EmptyResultDataAccessException ignored) {
      return new PosSettings(
          PosProfileReceiptSettings.defaults(), PosProfileNotificationSettings.defaults());
    }
  }

  private record PosSettings(
      PosProfileReceiptSettings receipt, PosProfileNotificationSettings notifications) {}

  private static String sellerDisplayName(String firstName, String lastName, String fallback) {
    var fullName =
        ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    return fullName.isBlank() ? fallback : fullName;
  }

  private static String currencyCode(Currency currency) {
    return currency == null ? null : currency.getCurrencyCode();
  }
}
