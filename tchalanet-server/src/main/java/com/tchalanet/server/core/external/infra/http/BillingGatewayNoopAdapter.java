package com.tchalanet.server.core.external.infra.http;

import com.tchalanet.server.core.external.port.out.BillingGatewayPort;
import com.tchalanet.server.core.external.port.out.BillingGatewayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BillingGatewayNoopAdapter implements BillingGatewayPort {

    @Override
    public void cancelSubscriptionImmediately(BillingGatewayRequest request) {
        log.info("BillingGateway(NOOP): cancelSubscriptionImmediately tenantId={} subscriptionId={}", request.tenantId(), request.subscriptionId());
    }

    @Override
    public void cancelSubscriptionAtPeriodEnd(BillingGatewayRequest request) {
        log.info("BillingGateway(NOOP): cancelSubscriptionAtPeriodEnd tenantId={} subscriptionId={}", request.tenantId(), request.subscriptionId());
    }

    @Override
    public void resumeSubscription(BillingGatewayRequest request) {
        log.info("BillingGateway(NOOP): resumeSubscription tenantId={} subscriptionId={}", request.tenantId(), request.subscriptionId());
    }

    @Override
    public void changeSubscriptionPlan(BillingGatewayRequest request) {
        log.info("BillingGateway(NOOP): changeSubscriptionPlan tenantId={} subscriptionId={} plan={}", request.tenantId(), request.subscriptionId(), request.planExternalKey());
    }
}
