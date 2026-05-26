package com.tchalanet.server.core.sales.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintCharge;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintBranding;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintDraw;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintIdentity;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLifecycle;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintMetadata;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintMoney;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintQrPayload;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintSellerContext;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintState;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintStateStatus;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.status.TicketPrintStatus;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.DrawLabelFormat;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketDrawLabelFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketVerificationUrlBuilder;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.infra.persistence.view.TicketPrintHeaderViewEntity;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketPrintViewMapper {

    private static final String DEFAULT_LOCALE = "fr";
    private static final String DEFAULT_TIMEZONE = "America/Port-au-Prince";
    private static final String DEFAULT_QR_PAYLOAD_VERSION = "v1";

    private final TicketVerificationUrlBuilder verificationUrlBuilder;
    private final TicketDrawLabelFormatter drawLabelFormatter;

    public TicketPrintView toPrintView(TicketPrintHeaderViewEntity header, Ticket ticket) {
        var verificationUrl = verificationUrlBuilder.buildUrl(header.getPublicCode());
        var drawLabel = resolveDrawChannelName(header);
        var locale = parseLocale(header.getLocale());
        var timezone = parseTimezone(header.getTimezone());
        var formattedDrawLabel = drawLabelFormatter.format(
            drawLabel,
            header.getDrawDate(),
            null,
            timezone,
            header.getScheduledAt(),
            locale,
            DrawLabelFormat.TICKET_SHORT
        );

        return new TicketPrintView(
            new TicketPrintIdentity(
                ticket.identity().id(),
                header.getTicketCode(),
                header.getPublicCode(),
                header.getVerificationCode()
            ),
            new TicketPrintLifecycle(
                header.getSaleStatus(),
                header.getResultStatus(),
                header.getSettlementStatus()
            ),
            new TicketPrintState(
                toPrintStateStatus(header.getPrintStatus()),
                header.getPrintCount(),
                header.getFirstPrintedAt(),
                header.getLastPrintedAt()
            ),
            new TicketPrintDraw(
                DrawId.of(header.getDrawId()),
                DrawChannelId.of(header.getDrawChannelId()),
                formattedDrawLabel,
                drawLabel,
                header.getDrawDate(),
                header.getScheduledAt(),
                header.getCutoffAt()
            ),
            new TicketPrintSellerContext(
                OutletId.of(header.getOutletId()),
                header.getOutletCode(),
                header.getOutletName(),
                TerminalId.of(header.getTerminalId()),
                header.getTerminalCode(),
                header.getTerminalLabel(),
                SalesSessionId.of(header.getSalesSessionId()),
                header.getSessionCode(),
                UserId.of(header.getSellerUserId()),
                header.getSellerDisplayName()
            ),
            new TicketPrintBranding(
                header.getTenantDisplayName(),
                header.getTenantReceiptHeader(),
                header.getTenantReceiptFooter(),
                header.getOutletReceiptHeader(),
                header.getOutletReceiptFooter()
            ),
            ticket.lines().stream()
                .sorted(java.util.Comparator.comparingInt(
                    com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine::lineNumber))
                .map(this::toPrintLine).toList(),
            new TicketPrintMoney(
                ticket.money().stakeAmount(),
                ticket.money().breakdown().charges().stream().map(this::toPrintCharge).toList(),
                ticket.money().breakdown().totalBuyerCharges(),
                ticket.money().totalAmount(),
                ticket.money().potentialPayoutAmount()
            ),
            new TicketPrintQrPayload(
                DEFAULT_QR_PAYLOAD_VERSION,
                header.getPublicCode(),
                header.getVerificationCode(),
                verificationUrl,
                // TODO: replace with cryptographically signed payload when QR signing is introduced.
                verificationUrl
            ),
            new TicketPrintMetadata(
                header.getPlacedAt(),
                locale,
                timezone,
                header.getSaleOrigin() == null ? TicketSaleChannel.POS_ONLINE : header.getSaleOrigin(),
                header.getCurrency(),
                Map.of()
            )
        );
    }

    private TicketPrintLine toPrintLine(TicketLine line) {
        return new TicketPrintLine(
            line.lineNumber(),
            line.gameCode(),
            line.betType(),
            line.betOption(),
            line.gameCode() == null ? null : line.gameCode().name(),
            line.selection() == null ? null : line.selection().displayLabel(),
            line.selection() == null ? null : line.selection().key().value(),
            line.oddsSnapshot(),
            line.stakeAmount(),
            line.potentialPayoutAmount(),
            line.origin(),
            line.pricingSource()
        );
    }

    private TicketPrintCharge toPrintCharge(TicketCharge charge) {
        return new TicketPrintCharge(
            charge.type(),
            charge.paidBy(),
            chargeLabel(charge.type()),
            charge.amount(),
            charge.waivedByRuleId()
        );
    }

    private String resolveDrawChannelName(TicketPrintHeaderViewEntity header) {
        return header.getDrawChannelDisplayName() == null
            ? header.getDrawChannelName()
            : header.getDrawChannelDisplayName();
    }

    private Locale parseLocale(String locale) {
        return locale == null || locale.isBlank() ? Locale.forLanguageTag(DEFAULT_LOCALE) : Locale.forLanguageTag(locale);
    }

    private ZoneId parseTimezone(String timezone) {
        return timezone == null || timezone.isBlank() ? ZoneId.of(DEFAULT_TIMEZONE) : ZoneId.of(timezone);
    }

    private String chargeLabel(TicketChargeType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case BUYER_SMS -> "Frais SMS";
            case BUYER_WHATSAPP -> "Frais WhatsApp";
            case BUYER_EMAIL, EMAIL_NOTIFICATION -> "Frais email";
        };
    }

    private TicketPrintStateStatus toPrintStateStatus(TicketPrintStatus status) {
        if (status == null) {
            return TicketPrintStateStatus.NOT_PRINTED;
        }
        return switch (status) {
            case NOT_PRINTED -> TicketPrintStateStatus.NOT_PRINTED;
            case PRINTED -> TicketPrintStateStatus.PRINTED;
            case REPRINTED -> TicketPrintStateStatus.REPRINTED;
        };
    }
}
