package com.tchalanet.server.core.session.internal.application.service.opening;

import com.tchalanet.server.core.session.internal.domain.model.SalesSessionOpeningContext;
import org.springframework.stereotype.Component;

import static com.tchalanet.server.core.session.internal.application.service.opening.SalesSessionOpeningDenialKind.CONFLICT;
import static com.tchalanet.server.core.session.internal.application.service.opening.SalesSessionOpeningDenialKind.FORBIDDEN;

@Component
public class SalesSessionOpeningEligibilityPolicy {

    public SalesSessionOpeningEligibility evaluate(SalesSessionOpeningContext ctx) {

        if (!ctx.tenantExists() || !ctx.tenantActive()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.tenant-inactive",
                "Tenant is not active",
                FORBIDDEN);
        }

        if (!ctx.userExists() || !ctx.userActive()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.user-inactive",
                "User is not active",
                FORBIDDEN);
        }

        if (!ctx.sellerExistsInTenant() || !ctx.sellerActiveInTenant()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.seller-inactive",
                "Seller is not active for this tenant",
                FORBIDDEN);
        }

        if (!ctx.sellerCanOpenPosSession()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.seller-not-allowed",
                "Seller cannot open POS session",
                FORBIDDEN);
        }

        if (!ctx.outletExists() || !ctx.outletBelongsToTenant()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.outlet-not-found",
                "Outlet not found",
                FORBIDDEN);
        }

        if (!ctx.outletActive() || ctx.outletBlocked()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.outlet-inactive",
                "Outlet is not active or is blocked",
                FORBIDDEN);
        }

        if (!ctx.terminalExists() || !ctx.terminalBelongsToTenant()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.terminal-not-found",
                "Terminal not found",
                FORBIDDEN);
        }

        if (!ctx.terminalBelongsToOutlet()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.terminal-outlet-mismatch",
                "Terminal does not belong to outlet",
                FORBIDDEN);
        }

        if (!ctx.terminalActive() || ctx.terminalBlocked()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.terminal-inactive",
                "Terminal is not active or is blocked",
                FORBIDDEN);
        }

        if (!ctx.terminalBound()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.terminal-not-bound",
                "Terminal is not bound to a device",
                FORBIDDEN);
        }

        if (!ctx.sellerAllowedForOutlet()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.seller-not-allowed-for-outlet",
                "Seller is not assigned to this outlet",
                FORBIDDEN);
        }

        if (!ctx.sellerAllowedForTerminal()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.seller-not-allowed-for-terminal",
                "Seller is not assigned to this terminal",
                FORBIDDEN);
        }

        if (!ctx.businessDayOpen()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.business-day-closed",
                "Business day is closed",
                FORBIDDEN);
        }

        if (ctx.currentOpenSessionId().isPresent()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.already-open",
                "A session is already open for this operational context",
                CONFLICT,
                ctx.currentOpenSessionId());
        }

        return SalesSessionOpeningEligibility.allowed();
    }
}
