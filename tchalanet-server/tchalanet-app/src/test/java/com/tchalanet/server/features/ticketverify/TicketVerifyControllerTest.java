package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.config.PublicTicketRateLimitProperties;
import com.tchalanet.server.core.sales.api.model.verification.CustomerTicketStatus;
import com.tchalanet.server.core.sales.api.model.verification.TicketVerificationView;
import com.tchalanet.server.features.ticketverify.infra.PublicTicketRateLimiter;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketVerifyControllerTest {

    @Test
    void returnsCanonicalProofWithNoStoreAndNoIndexHeaders() {
        var controller = new TicketVerifyController(
            new TicketVerifyService(new StubQueryBus()),
            new TicketVerifyMapper(),
            new PublicTicketRateLimiter(new PublicTicketRateLimitProperties(false, 1, 1)));

        var response = controller.verify(request("203.0.113.10"), new TicketVerifyRequest("ABCD-EFGH"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("noindex, nofollow", response.getHeaders().getFirst("X-Robots-Tag"));
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        assertEquals("0", response.getHeaders().getFirst("Expires"));
        assertNotNull(response.getBody());
        assertEquals("ABCD-EFGH", response.getBody().data().displayCode());
        assertEquals("Maryaj gratuit", response.getBody().data().lines().getFirst().promotionLabel());
    }

    @Test
    void rateLimitStopsBeforeTicketDataIsRead() {
        var queryBus = new StubQueryBus();
        var controller = new TicketVerifyController(
            new TicketVerifyService(queryBus),
            new TicketVerifyMapper(),
            new PublicTicketRateLimiter(new PublicTicketRateLimitProperties(true, 1, 1)));
        var request = request("203.0.113.11");

        controller.verify(request, new TicketVerifyRequest("ABCD-EFGH"));
        var thrown = assertThrows(
            ProblemRestException.class,
            () -> controller.verify(request, new TicketVerifyRequest("ABCD-EFGH")));

        assertEquals(429, thrown.getProblem().getStatus());
        assertEquals("ticket.verify.rate_limit_exceeded", thrown.getProblem().getDetail());
        assertEquals(1, queryBus.calls);
    }

    private static MockHttpServletRequest request(String remoteAddr) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static final class StubQueryBus implements QueryBus {
        private int calls;

        @Override
        @SuppressWarnings("unchecked")
        public <R> R ask(Query<R> query) {
            calls++;
            var htg = CurrencyCode.of("HTG");
            return (R) new TicketVerificationView(
                "ABCDEFGH",
                "ABCD-EFGH",
                CustomerTicketStatus.AWAITING_RESULT,
                new Money(BigDecimal.TEN, htg),
                null,
                Instant.parse("2026-05-27T09:00:00Z"),
                new TicketVerificationView.DrawInfoView(
                    "HAITI",
                    "Haiti",
                    LocalDate.parse("2026-05-27"),
                    Instant.parse("2026-05-27T20:00:00Z")),
                new TicketVerificationView.OutletInfoView("Outlet"),
                List.of(new TicketVerificationView.TicketLineView(
                    1,
                    "Maryaj",
                    "Maryaj",
                    "Straight",
                    "12-34",
                    new Money(BigDecimal.ZERO, htg),
                    new Money(BigDecimal.valueOf(100), htg),
                    true,
                    "Maryaj gratuit")));
        }
    }
}
