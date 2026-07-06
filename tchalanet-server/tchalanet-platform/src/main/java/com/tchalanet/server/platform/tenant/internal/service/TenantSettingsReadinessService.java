package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.platform.tenant.api.model.view.TenantBusinessCalendarRules;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalLocaleConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.api.model.view.TenantRuntimeView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView.ReadinessStatus;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView.SectionReadiness;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;

@Service
public class TenantSettingsReadinessService {

    private static final Set<String> PAPER_SIZES = Set.of("RECEIPT_58MM", "RECEIPT_80MM", "A4");
    private static final Set<String> PAID_BY_VALUES = Set.of("BUYER", "TENANT", "SELLER");

    public TenantSettingsReadinessView evaluate(TenantRuntimeView runtime, TenantInternalSettings settings) {
        var sections = List.of(
            identity(runtime),
            defaults(runtime),
            localePolicy(settings == null ? null : settings.locale()),
            print(settings == null ? null : settings.document()),
            sendOptions(runtime, settings == null ? null : settings.communication()),
            calendar(settings == null || settings.rules() == null ? null : settings.rules().businessCalendar())
        );
        var ready = sections.stream().allMatch(section -> section.status() == ReadinessStatus.READY);
        return new TenantSettingsReadinessView(ready, sections);
    }

    private SectionReadiness identity(TenantRuntimeView runtime) {
        var missing = new ArrayList<String>();
        if (runtime == null) {
            missing.add("settings.identity.runtime_missing");
        } else {
            requireText(runtime.displayName(), "settings.identity.display_name_missing", missing);
            if (runtime.timezone() == null) missing.add("settings.identity.timezone_missing");
            if (runtime.currency() == null) missing.add("settings.identity.currency_missing");
            requireText(runtime.defaultLanguage(), "settings.identity.default_language_missing", missing);
            requireText(runtime.defaultLocale(), "settings.identity.default_locale_missing", missing);
        }
        return section("identity", missing);
    }

    private SectionReadiness defaults(TenantRuntimeView runtime) {
        var missing = new ArrayList<String>();
        if (runtime == null) {
            missing.add("settings.defaults.runtime_missing");
        } else {
            var supported = runtime.supportedLocales() == null ? List.<String>of() : runtime.supportedLocales();
            if (supported.isEmpty()) {
                missing.add("settings.defaults.supported_languages_missing");
            }
            if (isBlank(runtime.defaultLanguage())) {
                missing.add("settings.defaults.default_language_missing");
            } else if (!supported.isEmpty() && !supported.contains(runtime.defaultLanguage())) {
                missing.add("settings.defaults.default_language_unsupported");
            }
            if (isBlank(runtime.defaultLocale())) {
                missing.add("settings.defaults.default_locale_missing");
            }
            if (runtime.currency() == null) {
                missing.add("settings.defaults.currency_missing");
            }
        }
        return section("defaults", missing);
    }

    private SectionReadiness localePolicy(TenantInternalLocaleConfig locale) {
        var missing = new ArrayList<String>();
        if (locale == null) {
            missing.add("settings.locale.policy_missing");
        } else {
            var supported = locale.effectiveSupportedLanguages();
            if (supported.isEmpty()) {
                missing.add("settings.locale.supported_languages_missing");
            }
            if (isBlank(locale.fallbackLanguage())) {
                missing.add("settings.locale.fallback_language_missing");
            } else if (!supported.isEmpty() && !supported.contains(locale.fallbackLanguage())) {
                missing.add("settings.locale.fallback_language_unsupported");
            }
        }
        return section("locale", missing);
    }

    private SectionReadiness print(TenantInternalDocumentConfig document) {
        var missing = new ArrayList<String>();
        var receipt = document == null ? null : document.receipt();
        if (receipt == null) {
            missing.add("settings.print.receipt_missing");
        } else if (receipt.enabled() == null) {
            missing.add("settings.print.receipt_enabled_missing");
        } else if (Boolean.TRUE.equals(receipt.enabled())) {
            requireText(receipt.defaultTemplateKey(), "settings.print.template_key_missing", missing);
            if (isBlank(receipt.defaultPaperSize())) {
                missing.add("settings.print.paper_size_missing");
            } else if (!PAPER_SIZES.contains(receipt.defaultPaperSize())) {
                missing.add("settings.print.paper_size_unsupported");
            }
            if (receipt.showQrCode() == null) {
                missing.add("settings.print.show_qr_code_missing");
            }
        }
        return section("print", missing);
    }

    private SectionReadiness sendOptions(TenantRuntimeView runtime, TenantInternalCommunicationConfig communication) {
        var missing = new ArrayList<String>();
        var delivery = communication == null ? null : communication.buyerTicketDelivery();
        if (delivery == null) {
            missing.add("settings.send.delivery_missing");
        } else {
            validateChannel("sms", delivery.sms(), runtime, missing);
            validateChannel("whatsapp", delivery.whatsapp(), runtime, missing);
            validateChannel("email", delivery.email(), runtime, missing);
        }
        return section("send", missing);
    }

    private void validateChannel(
        String channel,
        TenantInternalCommunicationConfig.DeliveryChannelConfig config,
        TenantRuntimeView runtime,
        List<String> missing
    ) {
        var prefix = "settings.send." + channel + ".";
        if (config == null) {
            missing.add(prefix + "missing");
            return;
        }
        if (config.enabled() == null) {
            missing.add(prefix + "enabled_missing");
            return;
        }
        if (!Boolean.TRUE.equals(config.enabled())) {
            return;
        }
        if (config.amount() == null || config.amount().compareTo(BigDecimal.ZERO) < 0) {
            missing.add(prefix + "amount_invalid");
        }
        if (isBlank(config.currency())) {
            missing.add(prefix + "currency_missing");
        } else if (!isCurrency(config.currency())) {
            missing.add(prefix + "currency_invalid");
        } else if (runtime != null && runtime.currency() != null
            && !runtime.currency().getCurrencyCode().equals(config.currency())) {
            missing.add(prefix + "currency_unsupported");
        }
        if (isBlank(config.paidBy())) {
            missing.add(prefix + "paid_by_missing");
        } else if (!PAID_BY_VALUES.contains(config.paidBy())) {
            missing.add(prefix + "paid_by_invalid");
        }
    }

    private SectionReadiness calendar(TenantBusinessCalendarRules calendar) {
        var missing = new ArrayList<String>();
        if (calendar == null) {
            missing.add("settings.calendar.rules_missing");
        } else {
            if (calendar.defaultOpen() == null) {
                missing.add("settings.calendar.default_open_missing");
            }
            if (calendar.closedWeekdays() == null) {
                missing.add("settings.calendar.closed_weekdays_missing");
            }
        }
        return section("calendar", missing);
    }

    private SectionReadiness section(String section, List<String> blockingReasons) {
        var status = blockingReasons.isEmpty() ? ReadinessStatus.READY : ReadinessStatus.ACTION_REQUIRED;
        return new SectionReadiness(section, status, blockingReasons, List.of());
    }

    private void requireText(String value, String reason, List<String> missing) {
        if (isBlank(value)) {
            missing.add(reason);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isCurrency(String value) {
        try {
            Currency.getInstance(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
