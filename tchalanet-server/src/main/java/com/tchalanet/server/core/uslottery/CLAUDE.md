# Claude — core.uslottery

Scope:

- Provider HTTP fetch only.
- Provider response normalization only.
- One provider client per source when response shape differs.

Out of scope:

- Tenant logic.
- Draw lifecycle.
- DrawResult persistence.
- Haiti projection.
- Ticket settlement.
- Frontend/mobile.

Rules:

- Provider clients do not know tenants.
- Provider clients do not know draw/draw_channel/draw_result.
- Provider clients do not own result_slot business decisions.
- Normalize provider output to internal provider result contract.
- Use provider game codes only:
  - PICK3, PICK4, NUMBERS, WIN4, CASH3, CASH4, DAILY4, etc.
- Avoid `US_*` business codes.
- Avoid composite mapping unless strictly necessary.
- Use injected `Clock`.
- Raw provider cache is infra-only.
- Cache is not source of truth.

Before editing:

- Inspect only the provider client/contract touched by the task.
- Do not inspect draw or sales unless compile/test failure requires it.
- Ask before expanding scope.

Output:

1. Provider behavior found
2. Mapping implemented
3. Edge cases
4. Tests
5. Handoff
