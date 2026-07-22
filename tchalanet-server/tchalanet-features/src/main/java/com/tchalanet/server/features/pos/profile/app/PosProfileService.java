package com.tchalanet.server.features.pos.profile.app;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalStatus;
import com.tchalanet.server.core.sellerterminal.api.query.GetSellerTerminalQuery;
import com.tchalanet.server.features.pos.profile.model.PosProfileCommercialInfo;
import com.tchalanet.server.features.pos.profile.model.PosProfileResponse;
import com.tchalanet.server.features.pos.profile.model.PosProfileSellerInfo;
import com.tchalanet.server.features.pos.profile.model.PosProfileSettingsInfo;
import com.tchalanet.server.features.pos.profile.model.PosProfileTerminalInfo;
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
            terminal.phoneNumber()),
        new PosProfileCommercialInfo(
            tenantId.value().toString(),
            ctx.effectiveTenantCode(),
            currencyCode(currency),
            terminal.commissionRate()),
        new PosProfileSettingsInfo(
            locale.toLanguageTag(),
            zone == null ? null : zone.getId(),
            currencyCode(currency),
            SUPPORTED_LOCALES));
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
