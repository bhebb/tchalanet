# Identity V0 Temporary Activation

## Why

Tchalanet V0 must onboard tenant admins and seller terminals without magic links or invitation email workflows. New users receive temporary credentials out of band, then complete a mandatory first-login activation before reaching their normal workspace.

## What

- Provisioning returns temporary credential status for the initial tenant admin instead of invitation/magic-link language.
- Runtime bootstrap routes users with activation flags to mandatory activation pages.
- Web adds activation surfaces for tenant admins and seller terminals.
- Server exposes a first-login completion endpoint for authenticated users.

## Impact

- Backend: tenant provisioning, identity state, runtime bootstrap, activation endpoint.
- Web: auth/session routing, activation routes/pages, i18n, provisioning result copy.
- Non-goals: transactional email/SMS, magic links, full admin password reset, autonomous seller PIN recovery.
