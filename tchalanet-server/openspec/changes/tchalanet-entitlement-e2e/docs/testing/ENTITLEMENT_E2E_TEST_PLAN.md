# Python E2E Scenario Checklist

## Files to add in existing E2E suite

Suggested names:

```text
tests/e2e/test_entitlement_capabilities.py
tests/e2e/test_entitlement_subscription_cache.py
tests/e2e/test_entitlement_quotas.py
tests/e2e/test_entitlement_multitenant_onboarding.py
tests/e2e/test_public_plan_pages.py
```

## Helper methods

```python
def create_tenant(...)
def onboard_tenant(...)
def apply_plan(tenant, plan_code)
def change_plan(tenant, plan_code)
def suspend_subscription(tenant)
def resume_subscription(tenant)
def get_capabilities(auth)
def create_terminal(auth, payload)
def create_outlet(auth, payload)
def create_tenant_user(auth, payload)
def get_public_pricing_page()
def get_admin_dashboard(auth)
```

## Assertions

- planCode matches expected.
- subscriptionActive matches expected.
- features map contains expected keys.
- limits map contains expected values.
- tenant A and tenant B snapshots differ correctly.
- cache invalidation works after subscription change.
- quota errors use stable error codes.
