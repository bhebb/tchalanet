## Tasks

- [x] Update plan and usage key constants for admin users and seller terminals.
- [x] Update plan seed entitlements and quotas directly in `V201`.
- [x] Remove CASHIER role/local user seed directly from `V202`.
- [x] Consolidate the `V202` permission catalog and role matrix for `SUPER_ADMIN`, `TENANT_OWNER`, and `TENANT_ADMIN`.
- [x] Add explicit permission guards for seller-terminal reads, draw channels, draw lifecycle, manual/override draw results, and ticket approve/reject/cancel flows.
- [x] Remove HT_NUMERO from game seeds and tenant provisioning defaults.
- [x] Add usage providers for every seeded quota with a concrete backing table.
- [x] Add quota guards on admin user, seller-terminal, and promotion rule creation.
- [ ] Add tenant-admin draw-channel quota enforcement when tenant-side channel creation is introduced.
