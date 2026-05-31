# Tasks — Réorganisation portail MkDocs

---

## T0 — OpenSpec skeleton ✅

- [x] Créer `proposal.md`
- [x] Créer `tasks.md`

## T1 — Audit nav (read-only) ✅

**Résultats :**
- `00-overview/architecture-map.md` dans le nav mais le fichier n'existe pas → lien cassé
- `00-overview/system-map.md`, `what-is-tchalanet.md`, `where-truth-lives.md` existent mais ne sont pas dans le nav
- `index.md` a déjà des reading paths mais utilise `pymdownx.tabbed` non configuré dans mkdocs.yml
- La section "Conventions" expose 15 fichiers individuels → trop granulaire
- Aucune section "Guide utilisateur" orientée opérateur/agent terrain
- `02-functional/guides/` inexistant — à créer

## T2 — Nouvelle nav `mkdocs.yml` ✅

- [x] Supprimer le lien cassé `architecture-map.md`
- [x] Ajouter section "Guide utilisateur" avec 2 sous-sections (opérateur, agent terrain)
- [x] Renommer "Flows" → "Métier" (flows système)
- [x] Remplacer "Conventions" granulaire → "Technique" avec hub `backend-conventions.md`
- [x] Exposer `what-is-tchalanet.md`, `system-map.md`, `where-truth-lives.md` dans le nav
- [x] Nettoyer not_in_nav (ajouter server-docs/**, mobile-docs/**, 02-functional/flows/index.md)

## T3 — Page d'introduction produit ✅

- `00-overview/what-is-tchalanet.md` existe déjà avec bon contenu — aucune modification nécessaire

## T4 — Homepage `index.md` ✅

- [x] Réécrire sans `pymdownx.tabbed` (ou ajouter l'extension)
- [x] Ajouter section client-facing (opérateur + agent terrain)
- [x] Conserver les reading paths existants
- [x] Supprimer/convertir les tables "Need / Start here / Canonical source" → `99-reference/index.md`

## T5 — Pages nouvelles ✅

- [x] `02-functional/guides/operator-admin-guide.md` — parcours opérateur
- [x] `02-functional/guides/field-agent-guide.md` — parcours agent terrain
- [x] `99-reference/backend-conventions.md` — hub conventions backend groupé
- [x] `00-overview/system-map.md` — amélioration contenu (ajout Mermaid)
- [x] `04-operations/index.md` — révision légère (hub court avec liens infra)

## T6 — Pattern flow (2e passe, optionnel)

- [ ] Améliorer `02-functional/flows/sell-ticket.md` avec pattern complet
- [ ] Améliorer `02-functional/flows/session-opening.md`
- [ ] Améliorer `02-functional/flows/payout-field-flow.md`

---

**Build verification :**
```bash
cd tchalanet-docs
venv/bin/mkdocs build --config-file mkdocs.yml
venv/bin/mkdocs serve --config-file mkdocs.yml
```
