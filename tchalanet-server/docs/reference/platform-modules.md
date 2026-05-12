# Living Reference — Platform Modules

This file is a living inventory. It may evolve without changing ADR-001.

## Placement matrix

| Capability     | Target                                       | Reason                                                     | Notes                                                           |
| -------------- | -------------------------------------------- | ---------------------------------------------------------- | --------------------------------------------------------------- |
| audit          | `platform.audit`                             | Stateful transversal compliance/traceability service       | Can use REQUIRES_NEW for failure audit.                         |
| access control | `platform.accesscontrol`                     | Application permission and role decision service           | Domain state rules remain in `core`.                            |
| tenant user    | `platform.identity`                          | User/app profile, tenant membership, user identity         | Use `identity`; avoids conflict with `platform.context`.        |
| tenant config  | `platform.tenantconfig`                      | Effective tenant configuration and overrides               | Catalog may hold settings definitions.                          |
| tenant theme   | `platform.tenanttheme`                       | Effective tenant theme and overrides                       | Catalog may hold global theme presets.                          |
| document       | `platform.document`                          | Document generation/storage/tracking                       | Core decides business content; platform produces document.      |
| communication  | `platform.communication`                     | Email/SMS/WhatsApp/provider delivery                       | Notification may use communication.                             |
| notification   | `platform.notification`                      | Notification inbox/preferences/state/routing               | Only create if product has notification concept beyond sending. |
| idempotence    | `common` + `platform.idempotence`            | Split primitive annotations/keys from persistence/workflow | Persistent records go to platform.                              |
| security       | `common.security` + `platform.accesscontrol` | Split technical Spring glue from app permission decisions  | Avoid business decisions in common.                             |

## Decision rule

A new capability goes to `platform` if it is stateful or lifecycle-bearing, transversal, and does not own a core business-critical invariant.

A new capability goes to `core` if a silent bug in it can cause direct financial loss, regulatory dispute, wrong winner, wrong payout, wrong draw/result, wrong settlement, or wrong limit decision.

A new capability goes to `catalog` if it is read-mostly reference data.

A new capability goes to `common` only if it is pure technical infrastructure with no persistent state and no application lifecycle.
