# Proposal — Réorganisation portail MkDocs

**Date :** 2026-05-30
**Type :** Documentation portal — navigation et guides utilisateur
**Branch :** `feature/reorg-doc`
**Statut :** En cours

---

## Why

Le portail MkDocs actuel est un entrepôt de fichiers, pas un portail guidé. La navigation mélange les audiences (client, dev, ops) et expose les fichiers au premier niveau plutôt que des parcours lisibles.

Le client (opérateur/admin et agent terrain) ne peut pas se débrouiller seul : il doit solliciter l'équipe pour trouver les flows et guides utilisateurs.

---

## What

Transformer le portail en site guidé orienté audience, en suivant le principe :

```
Page courte et guidée → explication → carte/flow → sources canoniques
```

**Audiences à servir :**
1. **Guide utilisateur** — opérateur/admin ET agent terrain (self-serve)
2. **Métier** — flows système complets (transversal)
3. **Architecture** — architectes, tech leads
4. **Technique** — développeurs (conventions)
5. **Opérations** — équipe ops/infra
6. **Référence interne** — guidelines, versions, agents

**Règles V1 :**
- Aucun déplacement de fichier existant
- Créer uniquement des pages hub/orientation nouvelles
- Ne pas dupliquer les règles canoniques
- Aucun mode strict pendant la refonte

---

## Impact

- `tchalanet-docs/mkdocs.yml` — nav restructurée
- `tchalanet-docs/docs/index.md` — homepage guidée
- `tchalanet-docs/docs/00-overview/system-map.md` — amélioration contenu
- `tchalanet-docs/docs/02-functional/guides/` — nouvelles pages guide
- `tchalanet-docs/docs/99-reference/backend-conventions.md` — nouveau hub

---

## Out of scope (V1)

- Déplacements de fichiers existants
- Traduction de contenu
- Design system (`07-design-system/`) — hors nav
- Specs granulaires near-code — hors nav
