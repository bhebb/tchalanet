# Opérations

## Ce que cette page répond

Comment faire tourner et déployer Tchalanet ?

Les procédures détaillées vivent dans `tchalanet-infra/docs/` — cette page oriente et pointe.

---

## Démarrage local

| Besoin | Source canonique |
|---|---|
| Démarrer la stack complète | `tchalanet-infra/docs/setup/` |
| Configuration des secrets (Doppler) | `tchalanet-infra/docs/setup/doppler.md` |
| Accès réseau local (`tchalanet.lan`) | `tchalanet-infra/docs/setup/lan.md` |
| Développement mobile depuis téléphone | `tchalanet-infra/docs/setup/mobile-dev.md` |

## Déploiement

| Besoin | Source canonique |
|---|---|
| Déploiement staging / production | `tchalanet-infra/docs/operations/deploy.md` |
| GitHub workflows (CI/CD) | `.github/workflows/` (root) |
| Hetzner infrastructure | `tchalanet-infra/docs/architecture/` |

## Supervision et maintenance

| Besoin | Source canonique |
|---|---|
| Backups et restauration | `tchalanet-infra/docs/operations/backup.md` |
| Troubleshooting edge service | `tchalanet-edge-service/` + `tchalanet-server/docs/` |
| Monitoring | `tchalanet-infra/docs/operations/` |

---

## Règle

Les runbooks détaillés restent dans `tchalanet-infra/docs/`.
MkDocs oriente — il ne copie pas les procédures.
