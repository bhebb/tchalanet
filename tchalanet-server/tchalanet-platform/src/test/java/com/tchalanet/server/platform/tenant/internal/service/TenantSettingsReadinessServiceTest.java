package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.platform.tenant.api.model.view.TenantBusinessCalendarRules;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalLocaleConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalRules;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.api.model.view.TenantRuntimeView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView.ReadinessStatus;
import com.tchalanet.server.platform.tenant.api.model.view.TenantSettingsReadinessView.SectionReadiness;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSettingsReadinessServiceTest {

    private final TenantSettingsReadinessService service = new TenantSettingsReadinessService();

    @Test
    void readyWithProvisionedDefaultsAndOptionalReceiptMessagesBlank() {
        var readiness = service.evaluate(runtime(), settings(
            receipt(true, null, "", "RECEIPT_80MM", true),
            delivery(channel(true, "5.00", "HTG", "BUYER")),
            calendar(true, Set.of())
        ));

        assertThat(readiness.ready()).isTrue();
        assertThat(readiness.sections())
            .allSatisfy(section -> assertThat(section.status()).isEqualTo(ReadinessStatus.READY));
    }

    @Test
    void disabledSendChannelDoesNotRequireFeeFields() {
        var delivery = new TenantInternalCommunicationConfig.BuyerTicketDeliveryConfig(
            channel(false, null, null, null),
            channel(false, null, null, null),
            channel(false, null, null, null)
        );

        var readiness = service.evaluate(runtime(), settings(
            receipt(true, null, null, "RECEIPT_80MM", true),
            delivery,
            calendar(true, Set.of(DayOfWeek.SUNDAY))
        ));

        assertThat(section(readiness, "send").status()).isEqualTo(ReadinessStatus.READY);
    }

    @Test
    void calendarReadyWithoutExceptionsOrHolidayRules() {
        var readiness = service.evaluate(runtime(), settings(
            receipt(true, null, null, "RECEIPT_80MM", true),
            delivery(channel(true, "0.00", "HTG", "TENANT")),
            calendar(true, Set.of())
        ));

        assertThat(section(readiness, "calendar").status()).isEqualTo(ReadinessStatus.READY);
    }

    @Test
    void defaultLanguageOutsideSupportedLanguagesBlocksDefaultsOnly() {
        var runtime = new TenantRuntimeView(
            "chez-toto",
            "chez-toto",
            "ACTIVE",
            ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            "es",
            "fr-HT",
            List.of("fr", "ht")
        );

        var readiness = service.evaluate(runtime, settings(
            receipt(true, null, null, "RECEIPT_80MM", true),
            delivery(channel(true, "0.00", "HTG", "TENANT")),
            calendar(true, Set.of())
        ));

        assertThat(readiness.ready()).isFalse();
        assertThat(section(readiness, "defaults").blockingReasons())
            .containsExactly("settings.defaults.default_language_unsupported");
        assertThat(section(readiness, "print").status()).isEqualTo(ReadinessStatus.READY);
    }

    @Test
    void enabledReceiptRequiresStructuralOptionsButNotHeaderFooter() {
        var readiness = service.evaluate(runtime(), settings(
            receipt(true, null, null, null, null),
            delivery(channel(true, "0.00", "HTG", "TENANT")),
            calendar(true, Set.of())
        ));

        assertThat(section(readiness, "print").blockingReasons())
            .containsExactlyInAnyOrder(
                "settings.print.paper_size_missing",
                "settings.print.show_qr_code_missing"
            );
    }

    private TenantRuntimeView runtime() {
        return new TenantRuntimeView(
            "chez-toto",
            "chez-toto",
            "ACTIVE",
            ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            "fr",
            "fr-HT",
            List.of("fr", "ht", "en")
        );
    }

    private TenantInternalSettings settings(
        TenantInternalDocumentConfig.ReceiptConfig receipt,
        TenantInternalCommunicationConfig.BuyerTicketDeliveryConfig delivery,
        TenantBusinessCalendarRules calendar
    ) {
        return new TenantInternalSettings(
            new TenantInternalCommunicationConfig(delivery),
            new TenantInternalDocumentConfig(receipt),
            new TenantInternalRules(calendar),
            new TenantInternalLocaleConfig(List.of("fr", "ht", "en"), "fr")
        );
    }

    private TenantInternalDocumentConfig.ReceiptConfig receipt(
        boolean enabled,
        String header,
        String footer,
        String paperSize,
        Boolean showQrCode
    ) {
        return new TenantInternalDocumentConfig.ReceiptConfig(
            enabled,
            header,
            footer,
            paperSize,
            showQrCode,
            "sales.ticket.receipt.v1"
        );
    }

    private TenantInternalCommunicationConfig.BuyerTicketDeliveryConfig delivery(
        TenantInternalCommunicationConfig.DeliveryChannelConfig channel
    ) {
        return new TenantInternalCommunicationConfig.BuyerTicketDeliveryConfig(channel, channel, channel);
    }

    private TenantInternalCommunicationConfig.DeliveryChannelConfig channel(
        boolean enabled,
        String amount,
        String currency,
        String paidBy
    ) {
        return new TenantInternalCommunicationConfig.DeliveryChannelConfig(
            enabled,
            amount == null ? null : new BigDecimal(amount),
            currency,
            paidBy
        );
    }

    private TenantBusinessCalendarRules calendar(Boolean defaultOpen, Set<DayOfWeek> closedWeekdays) {
        return new TenantBusinessCalendarRules(defaultOpen, closedWeekdays, List.of());
    }

    private SectionReadiness section(
        TenantSettingsReadinessView readiness,
        String section
    ) {
        return readiness.sections().stream()
            .filter(candidate -> candidate.section().equals(section))
            .findFirst()
            .orElseThrow();
    }
}
