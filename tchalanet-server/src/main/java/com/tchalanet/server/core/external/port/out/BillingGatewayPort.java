package com.tchalanet.server.core.external.port.out;

public interface BillingGatewayPort {
    void cancelSubscriptionImmediately(BillingGatewayRequest request);
    void cancelSubscriptionAtPeriodEnd(BillingGatewayRequest request);
    void resumeSubscription(BillingGatewayRequest request);
    void changeSubscriptionPlan(BillingGatewayRequest request);
}
