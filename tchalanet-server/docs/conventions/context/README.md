# Context — Index

- [`request-context.md`](./request-context.md) — Contexte universel d'exécution (tous rôles, tous flows d'entrée)
- [`operational-context.md`](./operational-context.md) — Contexte opérationnel POS/terrain (Seller, Admin POS uniquement)
- [`role-flows.md`](./role-flows.md) — Flows par rôle avec références croisées

**Règles fondamentales :**
- Request Context est universel. Operational Context est POS/terrain, pas global.
- Operational Context est attaché tôt mais validé tard, par action.
- Le tenant ne vient jamais du body client comme vérité.
- Un flow compose des sous-flows par liens. Il ne les recopie pas.

Vue pédagogique transverse : `tchalanet-docs/docs/02-functional/flows/role-login-flow.visual.html`
