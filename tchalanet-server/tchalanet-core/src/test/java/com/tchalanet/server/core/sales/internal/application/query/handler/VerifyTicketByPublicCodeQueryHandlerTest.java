package com.tchalanet.server.core.sales.internal.application.query.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameStatsView;
import com.tchalanet.server.catalog.game.api.model.GameSummaryView;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.catalog.i18n.api.model.I18nGlobalKeyStatsView;
import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.api.model.SearchI18nOverridesCriteria;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.config.TicketVisibilityProperties;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptI18nKeys;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.query.VerifyTicketByPublicCodeQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketVerificationProjection;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketVerificationReaderPort;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketPublicCodeFormatter;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketReceiptI18nResolver;
import com.tchalanet.server.core.sales.internal.domain.service.CustomerTicketStatusResolver;
import com.tchalanet.server.core.sales.internal.domain.service.TicketVisibilityPolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VerifyTicketByPublicCodeQueryHandlerTest {

    private static final TenantId TENANT_ID =
        TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));

    @AfterEach
    void clearContext() {
        TchContext.clear();
    }

    @Test
    void translatesReceiptPromotionKeysUsingCurrentContextLocale() {
        TchContext.set(context(Locale.ENGLISH));
        var handler = handler(new StubTicketVerificationReader(Instant.parse("2026-05-27T09:00:00Z")));

        var result = handler.handle(new VerifyTicketByPublicCodeQuery("ABCD-EFGH", "VCODE123"));

        assertThat(result.lines()).singleElement()
            .extracting(line -> line.promotionLabel())
            .isEqualTo("Free Maryaj");
    }

    @Test
    void unknownOrWrongVerificationCodeReturnsTicketNotFound() {
        var handler = handler((publicCode, verificationCode) -> Optional.empty());

        assertThatThrownBy(() -> handler.handle(new VerifyTicketByPublicCodeQuery("ABCD-EFGH", "WRONG")))
            .isInstanceOf(ProblemRestException.class)
            .extracting(ex -> ((ProblemRestException) ex).getProblem().getStatus())
            .isEqualTo(404);
    }

    @Test
    void hiddenTicketReturnsTicketNotFound() {
        var handler = handler(new StubTicketVerificationReader(Instant.parse("2025-01-01T09:00:00Z")));

        assertThatThrownBy(() -> handler.handle(new VerifyTicketByPublicCodeQuery("ABCD-EFGH", "VCODE123")))
            .isInstanceOf(ProblemRestException.class)
            .extracting(ex -> ((ProblemRestException) ex).getProblem().getStatus())
            .isEqualTo(404);
    }

    private static VerifyTicketByPublicCodeQueryHandler handler(TicketVerificationReaderPort reader) {
        return new VerifyTicketByPublicCodeQueryHandler(
            reader,
            new CustomerTicketStatusResolver(),
            new TicketVisibilityPolicy(
                new TicketVisibilityProperties(90),
                Clock.fixed(Instant.parse("2026-05-27T10:00:00Z"), ZoneOffset.UTC)
            ),
            new EmptyGameCatalog(),
            new TicketPublicCodeFormatter(),
            new TicketReceiptI18nResolver(new StubI18nOverridesCatalog())
        );
    }

    private static TchRequestContext context(Locale locale) {
        return new TchRequestContext(
            "demo",
            TENANT_ID.value(),
            "demo",
            TENANT_ID.value(),
            "10000000-0000-0000-0000-000000000001",
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            Set.of(TchRole.CASHIER),
            Set.of(),
            locale,
            "req-1",
            "127.0.0.1",
            "test",
            false,
            null,
            "active",
            ApiScope.PUBLIC,
            null,
            TENANT_ID,
            ZoneOffset.UTC,
            Currency.getInstance("USD"),
            null
        );
    }

    private record StubTicketVerificationReader(Instant placedAt) implements TicketVerificationReaderPort {
        @Override
        public Optional<TicketVerificationProjection> findByPublicCodeAndVerificationCode(
            String publicCode,
            String verificationCode
        ) {
            var usd = CurrencyCode.of("USD");
            return Optional.of(new TicketVerificationProjection(
                TENANT_ID,
                publicCode,
                "ABCD-EFGH",
                TicketSaleStatus.APPROVED,
                TicketResultStatus.PENDING,
                TicketSettlementStatus.NOT_SETTLED,
                placedAt,
                new Money(BigDecimal.TEN, usd),
                null,
                new TicketVerificationProjection.DrawProjection(
                    "HAITI",
                    "Haiti",
                    LocalDate.parse("2026-05-27"),
                    Instant.parse("2026-05-27T20:00:00Z")
                ),
                new TicketVerificationProjection.OutletProjection("Outlet"),
                List.of(new TicketVerificationProjection.LineProjection(
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "12-34",
                    new Money(BigDecimal.ZERO, usd),
                    new Money(BigDecimal.valueOf(100), usd),
                    true,
                    TicketReceiptI18nKeys.PROMOTION_FREE_GAME_LINE
                ))
            ));
        }
    }

    private static final class StubI18nOverridesCatalog implements I18nOverridesCatalog {
        @Override
        public TchPage<I18nOverrideView> search(SearchI18nOverridesCriteria criteria, TchPageRequest pageRequest) {
            return TchPage.of(List.of(), 0, 20, 0, 0, true, false, false);
        }

        @Override
        public Optional<I18nOverrideView> findByKey(String locale, String i18nKey) {
            return Optional.empty();
        }

        @Override
        public Map<String, String> resolveLocale(String locale, TchRequestContext ctx) {
            return values(locale);
        }

        @Override
        public Map<String, String> resolveLocaleForTenant(String locale, TenantId tenantId) {
            return values(locale);
        }

        @Override
        public com.tchalanet.server.catalog.i18n.api.model.I18nBundleView loadBundle(
                String locale, java.util.Set<com.tchalanet.server.catalog.i18n.api.model.I18nSurface> surfaces) {
            return new com.tchalanet.server.catalog.i18n.api.model.I18nBundleView(locale, Map.of());
        }

        @Override
        public I18nGlobalKeyStatsView keyStats() {
            return new I18nGlobalKeyStatsView(0, 0, 0);
        }

        private Map<String, String> values(String locale) {
            if ("en".equals(locale)) {
                return Map.of(TicketReceiptI18nKeys.PROMOTION_FREE_GAME_LINE, "Free Maryaj");
            }
            return Map.of(TicketReceiptI18nKeys.PROMOTION_FREE_GAME_LINE, "Maryaj gratuit");
        }
    }

    private static final class EmptyGameCatalog implements GameCatalog {
        @Override
        public List<GameView> listActive() {
            return List.of();
        }

        @Override
        public Optional<GameView> findByCode(String code) {
            return Optional.empty();
        }

        @Override
        public Optional<GameView> findById(GameId id) {
            return Optional.empty();
        }

        @Override
        public GameStatsView stats() {
            return null;
        }

        @Override
        public List<GameSummaryView> listRecent(int limit) {
            return List.of();
        }
    }
}
