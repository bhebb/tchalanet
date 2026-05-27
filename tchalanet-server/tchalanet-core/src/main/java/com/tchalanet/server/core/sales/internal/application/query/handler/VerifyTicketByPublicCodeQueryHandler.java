package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;
import com.tchalanet.server.core.sales.api.model.verification.TicketVerificationView;
import com.tchalanet.server.core.sales.api.query.VerifyTicketByPublicCodeQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketVerificationProjection;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketVerificationReaderPort;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketPublicCodeFormatter;
import com.tchalanet.server.core.sales.internal.domain.service.CustomerTicketStatusResolver;
import com.tchalanet.server.core.sales.internal.domain.service.TicketVisibilityPolicy;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@UseCase
@RequiredArgsConstructor
public class VerifyTicketByPublicCodeQueryHandler
    implements QueryHandler<VerifyTicketByPublicCodeQuery, TicketVerificationView> {

    private final TicketVerificationReaderPort reader;
    private final CustomerTicketStatusResolver statusResolver;
    private final TicketVisibilityPolicy visibilityPolicy;
    private final GameCatalog gameCatalog;
    private final TicketPublicCodeFormatter publicCodeFormatter;
    private final TicketReceiptI18nResolver i18nResolver;

    @Override
    public TicketVerificationView handle(VerifyTicketByPublicCodeQuery query) {
        var projection = reader.findByPublicCodeAndVerificationCode(
                publicCodeFormatter.normalize(query.publicCode()),
                query.verificationCode()
            )
            .orElseThrow(() -> ProblemRest.notFound("ticket.not_found"));

        if (!visibilityPolicy.isPubliclyVisible(projection.placedAt())) {
            throw ProblemRest.notFound("ticket.not_found");
        }

        var status = statusResolver.resolve(
            projection.saleStatus(),
            projection.resultStatus(),
            projection.settlementStatus()
        );

        var winningAmount = isWinning(status) ? projection.winningAmount() : null;

        return new TicketVerificationView(
            projection.publicCode(),
            projection.displayCode(),
            status,
            projection.totalAmount(),
            winningAmount,
            projection.placedAt(),
            new TicketVerificationView.DrawInfoView(
                projection.draw().drawChannelName(),
                projection.draw().drawChannelLabel(),
                projection.draw().drawDate(),
                projection.draw().scheduledAt()
            ),
            projection.outlet() != null
                ? new TicketVerificationView.OutletInfoView(projection.outlet().outletName())
                : null,
            projection.lines().stream()
                .map(line -> toLineView(projection, line))
                .toList()
        );
    }

    private boolean isWinning(CustomerTicketStatus status) {
        return status == CustomerTicketStatus.WON_CLAIMABLE
            || status == CustomerTicketStatus.WON_PAID
            || status == CustomerTicketStatus.CORRECTED;
    }

    private TicketVerificationView.TicketLineView toLineView(
        TicketVerificationProjection projection,
        TicketVerificationProjection.LineProjection line
    ) {
        var gameDisplayName = line.gameCode() != null
            ? firstNonBlank(
                line.gameLabel(),
                gameCatalog.findByCode(line.gameCode().name())
            .map(g -> g.name())
            .orElse(line.gameCode().name()))
            : null;
        var betTypeLabel = firstNonBlank(line.betTypeLabel(), line.betType() != null ? line.betType().name() : null);
        return new TicketVerificationView.TicketLineView(
            line.lineNumber(),
            gameDisplayName,
            betTypeLabel,
            line.optionLabel(),
            line.displaySelection(),
            line.stake(),
            line.potentialPayout(),
            line.promotional(),
            promotionLabel(projection, line)
        );
    }

    private String promotionLabel(
        TicketVerificationProjection projection,
        TicketVerificationProjection.LineProjection line
    ) {
        if (line.promotionLabel() == null || line.promotionLabel().isBlank()) {
            return line.promotionLabel();
        }
        if (!line.promotionLabel().startsWith("receipt.")) {
            return line.promotionLabel();
        }
        var ctx = TchContext.currentOrNull();
        var locale = ctx != null && ctx.locale() != null ? ctx.locale() : Locale.FRENCH;
        return i18nResolver.resolve(locale, projection.tenantId()).text(line.promotionLabel());
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
