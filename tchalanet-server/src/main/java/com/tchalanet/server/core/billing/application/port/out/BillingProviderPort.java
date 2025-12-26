package com.tchalanet.server.core.billing.application.port.out;

public interface BillingProviderPort {
  void cancelImmediately(BillingParams params);

  void cancelAtPeriodEnd(BillingParams params);

  void resume(BillingParams params);

  void changePlan(BillingParams params, String planExternalKey);
}
