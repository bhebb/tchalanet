# OpenSpec Pack — SellerTerminal V0 + Flutter POS + Admin Dashboard

This folder contains three proposed OpenSpec changes:

```text
openspec/changes/seller-terminal-v0/
openspec/changes/flutter-terminal-pos-v0/
openspec/changes/admin-dashboard-stats-v0/
```

Recommended implementation order:

1. Finish `provider-neutral-access-context-v1`.
2. Implement `seller-terminal-v0`.
3. Implement `flutter-terminal-pos-v0` backend contracts and then Flutter client.
4. Implement `admin-dashboard-stats-v0`.

Domain decision:

```text
SellerTerminal belongs in core.terminal.
```

Reason:

- it is the selling actor;
- it controls whether sales are allowed;
- it carries commission, odds profile and limit profile;
- it has direct financial/control impact;
- it is not just identity, not catalog, and not a feature-owned concept.
