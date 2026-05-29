package com.tchalanet.server.core.session.internal.application.service.opening;

import com.tchalanet.server.core.session.internal.domain.model.SalesSessionOpeningContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SalesSessionOpeningEligibilityPolicy {

    public SalesSessionOpeningEligibility evaluate(SalesSessionOpeningContext ctx) {

        if (!ctx.tenantExists() || !ctx.tenantActive()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.tenant-inactive",
                "Tenant is not active");
        }

        if (!ctx.userExists() || !ctx.userActive()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.user-inactive",
                "User is not active");
        }

        if (!ctx.sellerExistsInTenant() || !ctx.sellerActiveInTenant()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.seller-inactive",
                "Seller is not active for this tenant");
        }

        if (!ctx.sellerCanOpenPosSession()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.seller-not-allowed",
                "Seller cannot open POS session");
        }

        if (!ctx.outletExists() || !ctx.outletBelongsToTenant()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.outlet-not-found",
                "Outlet not found");
        }

        if (!ctx.outletActive() || ctx.outletBlocked()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.outlet-inactive",
                "Outlet is not active or is blocked");
        }

        if (!ctx.terminalExists() || !ctx.terminalBelongsToTenant()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.terminal-not-found",
                "Terminal not found");
        }

        if (!ctx.terminalBelongsToOutlet()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.terminal-outlet-mismatch",
                "Terminal does not belong to outlet");
        }

        if (!ctx.terminalActive() || ctx.terminalBlocked()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.terminal-inactive",
                "Terminal is not active or is blocked");
        }

        if (!ctx.terminalBound()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.terminal-not-bound",
                "Terminal is not bound to a device");
        }

        if (!ctx.sellerAllowedForOutlet()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.seller-not-allowed-for-outlet",
                "Seller is not assigned to this outlet");
        }

        if (!ctx.sellerAllowedForTerminal()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.seller-not-allowed-for-terminal",
                "Seller is not assigned to this terminal");
        }

        if (!ctx.businessDayOpen()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.business-day-closed",
                "Business day is closed");
        }

        if (ctx.currentOpenSessionId().isPresent()) {
            return SalesSessionOpeningEligibility.denied(
                "sales.session.already-open",
                "A session is already open for this operational context",
                ctx.currentOpenSessionId(),
                Map.of("salesSessionId", ctx.currentOpenSessionId().get().value()));
        }

        return SalesSessionOpeningEligibility.allowed();
    }
}
