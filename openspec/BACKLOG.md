# Backlog observations — entrées pour spec transverse

> Fichier de collecte. Rempli au fil des analyses de code et de docs.  
> Chaque item est une observation brute : glitch, incohérence, trou, ou dette.  
> Source pour le prochain spec transverse.

---

## Convention de nommage near-code

✅ Établie, documentée dans `doc-policy.md` et dans l'ARCHITECTURE.md de `tchalanet-server` (TODO).

| Slice | Préfixe near-code | Couverture |
|---|---|---|
| `tchalanet-core` | `DOMAIN_*.md` | ✅ 17 packages couverts |
| `tchalanet-catalog` | `CATALOG_*.md` | ✅ 6 packages couverts |
| `tchalanet-platform` | `PLATFORM_*.md` | ✅ 13 packages couverts |
| `tchalanet-features` | `FEATURE_*.md` | ✅ 8 features couvertes |

---

## Flows fonctionnels

✅ Tous les flows référencés dans `role-flows.md` sont créés.

---

## DOMAIN_*.md — items en attente

### tchalanet-core — bloqué par active changes

| Domaine | Bloqué par |
|---|---|
| `core.seller` | Change `tchalanet-commercial-network-v1` |
| `core.promotion` | Change `tchalanet-commercial-network-v1` + `tchalanet-promotion-integration-pack` |

### Incohérence à corriger après spec

| Fichier | Problème |
|---|---|
| `core/terminal/DOMAIN_TERMINAL.md` | `TerminalStatus` dans le doc vs `TerminalState` dans le code — attente de `terminal-reorg-security` |

---

## Liens stale dans les docs existants

| Fichier | Problème |
|---|---|
| `FEATURE_CASHIER.md` | `print-ticket/` est un dossier — le lien devrait pointer vers `print-ticket/00-index.md` |

---

## Outils référencés non implémentés

| Outil | Référencé dans | Description |
|---|---|---|
| `pnpm docs:check` | `doc-policy.md` | Validation automatique — jamais implémenté |

---

## Openspec changes offlinesync — deux changes actives

`add-offlinesync` et `add-offlinesync-module` couvrent le même domaine.  
Les deux ont des tâches ouvertes — ce sont des specs actives, pas des archives.  
Le `DOMAIN_OFFLINESYNC.md` dans les changes est un doc de conception (pas un near-code dupliqué).  
Source canonique near-code : `tchalanet-core/.../core/offlinesync/DOMAIN_OFFLINESYNC.md`.  
**Action à la livraison** : vérifier si les deux changes peuvent être fusionnées avant merge.

---

## tchalanet-infra — gaps documentaires

Audit 2026-05-30 :

| Constat | Impact |
|---|---|
| Pas d'`docs/ARCHITECTURE.md` | Pas de vue globale de l'infra (services, réseau, volumes) |
| Pas de `docs/conventions/` | Pas de règles formalisées (nommage, env vars, volumes) |
| 15 docs opérationnels (~3800 lignes), datés nov 2025 | Probablement stales sur certains détails |
| Overlap entre `DEPLOYMENT.md` et `OPERATIONS.md` | Deux sources pour le même sujet |
| `DEMARRAGE.md` — état "100% opérationnel" nov 2025 | Potentiellement périmé |
| `mobile-distribution-v0.md` — "v0" dans le nom | À renommer ou archiver si supersédé |

**À créer :**
- `tchalanet-infra/docs/ARCHITECTURE.md` — services Docker, réseau Traefik, volumes, Keycloak, Doppler
- `tchalanet-infra/docs/conventions/` — conventions env vars, secrets, nommage services

---

## Features — contenu à vérifier

| Feature | Statut |
|---|---|
| `FEATURE_REPORTING.md` | Existe — contenu à vérifier |
| `FEATURE_STATS.md` | Existe — contenu à vérifier |
| `FEATURE_PUBLICDRAWRESULTS.md` | Existe — contenu à vérifier |
| `FEATURE_OPS.md` | Existe — contenu à vérifier |
| `FEATURE_TENANTADMIN_OFFLINE.md` | Existe — probablement stub |

---

## Observations mineures

- `tchalanet-server/docs/ARCHITECTURE.md` — à mettre à jour avec la convention nommage near-code (DOMAIN/CATALOG/PLATFORM/FEATURE)
- `tchalanet-web/.prettierignore` et `.prettierrc` modifiés sur `chore/ai-agent-setup` — vérifier si intentionnel
- `SELLER_GUIDE.md` en FR uniquement — version créole haïtien à prévoir (V2 si app bilingue)
