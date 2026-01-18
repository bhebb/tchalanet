# 01 - Architecture

Cette section décrit l’architecture de Tchalanet au niveau **système**.
Elle reste volontairement **stable** et **courte**.

- Les détails d’implémentation vivent près du code :
  - backend : `tchalanet-server/docs/`
  - infra : `tchalanet-infra/docs/`
  - web : `apps/tchalanet-web/*.md` + `libs/**/README.md`
- Les règles IA / SDD vivent dans OpenSpec :
  - `openspec/project.md`
  - `openspec/context/*`

## Entrées principales

- **System overview** : vue globale composants et flux
- **Security model** : scopes API, auth, RLS, public endpoints
- **Backend map** : où trouver quoi côté server
- **Frontend map** : où trouver quoi côté web/mobile
- **Infra map** : où trouver quoi côté infra/devops

## Règle

Si ce document devient trop détaillé → déplacer le détail vers “near-code docs”
et laisser ici un pointeur.

**Vue d'ensemble système Tchalanet**

Cette section contient les "maps" architecture — des guides rapides pour savoir où trouver quoi dans le code et comment les modules interagissent.

---

## 📋 Documents

### [Vue d'ensemble système](system-overview.md)

Architecture globale, stack technique, patterns principaux

### [Backend Map](backend-map.md)

Organisation code backend (`common`, `core`, `features`, `catalog`), hexagonal + CQRS

### [Frontend Map](frontend-map.md)

Organisation code web (Angular/Nx) + mobile (Ionic), BFF PageModel, widgets

### [Infra Map](infra-map.md)

Infrastructure (Docker, PostgreSQL, Keycloak, Traefik), déploiement

### [Security Model](security-model.md)

Multi-tenant (RLS), auth (Keycloak), permissions, context

---

## 🎯 Principe

Ces "maps" sont des **pointeurs** vers les docs détaillées (proches du code).

Pour des détails techniques précis, voir :

- Backend : [99-links/backend.md](../99-links/backend.md) → `tchalanet-server/docs/`
- Web : [99-links/web.md](../99-links/web.md) → `apps/tchalanet-web/docs/`
- Mobile : [99-links/mobile.md](../99-links/mobile.md) → `apps/tchalanet-mobile/docs/`
- Infra : [99-links/infra.md](../99-links/infra.md) → `tchalanet-infra/docs/`

---

**Dernière mise à jour** : 2026-01-17
