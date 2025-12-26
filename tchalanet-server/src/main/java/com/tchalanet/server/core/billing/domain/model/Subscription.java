package com.tchalanet.server.core.billing.domain.model;

import com.tchalanet.server.common.types.id.PlanId;
import com.tchalanet.server.common.types.id.SubscriptionId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.billing.domain.exception.SubscriptionAlreadyCanceledException;
import com.tchalanet.server.core.billing.domain.exception.SubscriptionCannotBeCanceledException;
import com.tchalanet.server.core.billing.domain.exception.SubscriptionCannotBeResumedException;
import com.tchalanet.server.core.billing.domain.exception.SubscriptionCannotBeSuspendedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public record Subscription(
    SubscriptionId id,
    TenantId tenantId,
    PlanId planId,
    SubscriptionStatus status,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    boolean cancelAtPeriodEnd,
    BillingProvider billingProvider,
    String billingExternalId,
    Map<String, Object> meta,
    long version) {

    public Subscription scheduleCancellation(Instant now) {
        if (status == SubscriptionStatus.CANCELED) throw new SubscriptionAlreadyCanceledException(id);
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.TRIALING) {
            throw new SubscriptionCannotBeCanceledException(id, status);
        }
        return new Subscription(
            id, tenantId, planId, status,
            currentPeriodStart, currentPeriodEnd,
            true, billingProvider, billingExternalId, meta, version);
    }

    public Subscription cancelNow(Instant now) {
        if (status == SubscriptionStatus.CANCELED) throw new SubscriptionAlreadyCanceledException(id);
        Instant at = now != null ? now : Instant.now();
        return new Subscription(
            id, tenantId, planId, SubscriptionStatus.CANCELED,
            currentPeriodStart, at,
            false, billingProvider, billingExternalId, meta, version);
    }


    public Subscription resume(Instant now) {
        if (status != SubscriptionStatus.SUSPENDED) {
            throw new SubscriptionCannotBeResumedException(SubscriptionId.nullableOf(id.value()));
        }
        return withStatus(SubscriptionStatus.ACTIVE);
    }

    public Subscription suspend(Instant now) {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionCannotBeSuspendedException(SubscriptionId.nullableOf(id.value()), status);
        }
        return withStatus(SubscriptionStatus.SUSPENDED);
    }

    public Subscription changePlan(Plan newPlan, Instant now) {
        // V1: change immédiat sans prorata
        return new Subscription(
            id, tenantId, newPlan.id(), status, currentPeriodStart, currentPeriodEnd,
            cancelAtPeriodEnd, billingProvider, billingExternalId, meta, version);
    }

    public Subscription renew(Instant newEnd) {
        return new Subscription(
            id, tenantId, planId, status, currentPeriodStart, newEnd,
            cancelAtPeriodEnd, billingProvider, billingExternalId, meta, version);
    }

    public Subscription updateBilling(BillingProvider provider, String externalId, Instant start, Instant end, Map<String, Object> meta) {
        return new Subscription(
            id, tenantId, planId, status, start, end,
            cancelAtPeriodEnd, provider, externalId, meta, version);
    }

    public Subscription withStatus(SubscriptionStatus newStatus) {
        return new Subscription(
            id, tenantId, planId, newStatus, currentPeriodStart, currentPeriodEnd,
            cancelAtPeriodEnd, billingProvider, billingExternalId, meta, version);
    }

    public static Subscription start(SubscriptionId subscriptionId, TenantId tenantId, PlanId planId, Instant now, BillingProvider provider) {
        return new Subscription(
            subscriptionId,
            tenantId,
            planId,
            SubscriptionStatus.ACTIVE,
            now,
            now.plus(30, ChronoUnit.DAYS),
            false,
            provider,
            null,
            new HashMap<>(),
            0
        );
    }
}
