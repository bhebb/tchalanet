package com.tchalanet.server.core.billing.infra.provider;

import com.tchalanet.server.core.billing.application.port.out.BillingParams;
import com.tchalanet.server.core.billing.application.port.out.BillingProviderPort;
import com.tchalanet.server.core.external.port.out.BillingGatewayPort;
import com.tchalanet.server.core.external.port.out.BillingGatewayRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalBillingProviderAdapter implements BillingProviderPort {

  private final BillingGatewayPort billingGateway;

  @Override
  public void cancelImmediately(BillingParams params) {
    var request = BillingGatewayRequest.of(params.tenantId(), params.subscriptionId());
    billingGateway.cancelSubscriptionImmediately(request);
  }

  @Override
  public void cancelAtPeriodEnd(BillingParams params) {
    var request = BillingGatewayRequest.of(params.tenantId(), params.subscriptionId());
    billingGateway.cancelSubscriptionAtPeriodEnd(request);
  }

  @Override
  public void resume(BillingParams params) {
    var request = BillingGatewayRequest.of(params.tenantId(), params.subscriptionId());
    billingGateway.resumeSubscription(request);
  }

  @Override
  public void changePlan(BillingParams params, String planExternalKey) {
    var request =
        BillingGatewayRequest.of(params.tenantId(), params.subscriptionId(), planExternalKey);
    billingGateway.changeSubscriptionPlan(request);
  }
}
