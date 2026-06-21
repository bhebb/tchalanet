# 02 - Fonctionnel

**Domaines métier et workflows transverses**

Cette section contient la documentation **fonctionnelle** (agnostique de l'implémentation technique).

Cette section décrit le **métier** de Tchalanet, indépendamment de la stack.

Objectif :

- lisible par business / produit / dev
- stable
- sans détails techniques inutiles

## Deux vues

- **Domaines** : responsabilités, invariants, concepts
- **Flows** : scénarios bout-en-bout (sell ticket, verify ticket, draw execution…)
- **Features (BFF)** : orchestration cross-domain côté web/mobile (pages, widgets)
- **Catalogs** : référentiels et lookup stables (read-mostly)

## Source of truth (implémentation)

- Backend domains: `tchalanet-server/src/**/DOMAIN_*.md`
- Backend features: `tchalanet-server/src/**/FEATURE_*.md`

---

## 📋 Sous-sections

### [Domains (Domaines métier)](domains/index.md)

Bounded contexts critiques : sales, draws, limits, accesscontrol, sellerterminal, catalog

### [Features (BFF)](features/index.md)

Orchestration côté serveur pour pages/web mobile (PageModel, PublicDraw, News, PrivateDashboard, PublicHome, Stats, i18n, Notifications, Reporting)

### [Flows (Workflows transverses)](flows/index.md)

Workflows utilisateur croisant plusieurs modules : vente ticket, vérification publique, exécution tirage

---

## ℹ️ Catalogs (référentiels)

Les catalogues listent des données de référence stables (read-mostly), sans invariants critiques :

- Game (codes jeux, bet types)
- Pricing (multipliers, odds)
- DrawResult (résultats publiés)
- ResultSlot (slots globaux: provider/timezone/drawTime/daysOfWeek)

Voir sous-sections Domaines → Catalog et les pages dédiées.

---

## 🎯 Principe

Les docs fonctionnelles :

- ✅ Décrivent **contrats**, **états**, **workflows** (diagrammes, tableaux)
- ✅ Sont lisibles par business / product / dev
- ❌ **Ne contiennent PAS** de code Java/TS/SQL
- ✅ Référencent les implémentations techniques via liens

Pour voir l'implémentation d'un domaine :

- Backend : [99-links/backend.md](../99-links/backend.md) → `tchalanet-server/src/**/DOMAIN_*.md`
- Features (BFF) : [99-links/backend.md](../99-links/backend.md) → `tchalanet-server/src/**/FEATURE_*.md`
- Web/Mobile : [99-links/web.md](../99-links/web.md) → `tchalanet-web/` et `tchalanet-mobile/`

---

**Dernière mise à jour** : 2026-01-17
