# Proposal — Portail docs public/testeurs et documentation technique

**Date :** 2026-07-23
**Type :** Documentation portal — MkDocs, publication, accès par audience
**Statut :** Draft

---

## Why

Une livraison mobile + admin est prévue d'ici le week-end. Les testeurs doivent
avoir accès à une documentation claire pour comprendre Tchalanet, installer ou
utiliser le mobile, valider les parcours admin, et savoir quoi vérifier.

Le portail MkDocs actuel est utile comme base interne, mais il mélange :

- contenu testeur/client ;
- parcours métier ;
- documentation technique ;
- architecture, conventions, références internes et OpenSpec.

Publier tout le site tel quel sur `docs.tchalanet.com` exposerait trop de
contenu technique et rendrait la lecture confuse pour des testeurs. À l'inverse,
retirer la documentation technique du repo priverait l'équipe d'un espace de
travail utile.

---

## What

Mettre en place une stratégie MkDocs à deux surfaces :

1. **Docs public/testeurs** — site court, guidé, déployable manuellement sur
   Cloudflare Pages et exposable sur `docs.tchalanet.com`.
2. **Docs technique/interne** — documentation complète ou technique, non
   déployée publiquement par défaut, ou protégée par Cloudflare Access / réseau
   privé / authentification.

Le contenu public doit guider les rôles qui valident la livraison :

- testeur admin ;
- owner / propriétaire de santral ;
- super admin ;
- vendeur / terminal POS.

Le contenu technique doit conserver les parcours d'implémentation et
d'exploitation :

- architecture ;
- backend / web / mobile / edge ;
- infra et opérations ;
- OpenSpec ;
- conventions et références internes.

La séparation retenue pour la livraison week-end est une séparation physique :

```text
tchalanet-docs/
├── mkdocs.public.yml
├── mkdocs.internal.yml
├── docs-public/
│   ├── index.md
│   ├── admin/
│   ├── owner/
│   ├── super-admin/
│   ├── pos/
│   ├── validation/
│   └── faq.md
└── docs/
    ├── 01-architecture/
    ├── 02-functional/
    ├── 04-operations/
    ├── 06-openspec/
    ├── 99-reference/
    └── ...
```

Le build public doit utiliser `docs_dir: docs-public`. Cela évite que MkDocs
copie, indexe ou expose par URL directe des pages techniques non présentes dans
la navigation.

---

## Target Shape

### Build public/testeurs

But : donner une documentation utilisable sans contexte technique.

Contenu attendu :

- Accueil testeurs : objectifs de la livraison, environnement, liens utiles.
- Bannière d'environnement sur les pages testeurs :
  `STAGING`, données de test uniquement, aucun ticket/montant réel, version
  testée et date de mise à jour.
- Guide Admin : connexion, configuration générale, jeux, canaux de tirage,
  résultats, tickets, rapports de base.
- Guide Owner : configuration entreprise/santral, terminaux POS, rôles,
  limites, suivi opérationnel.
- Guide Super Admin : support tenant, activation provider, override encadré,
  contrôle des statuts.
- Guide Vendeur / POS : connexion terminal, vente, impression/reçu,
  consultation ticket, actions permises.
- Validation : scénarios de test, données attendues, comment signaler un bug.
- FAQ courte : accès, PIN, erreur fréquente, environnement staging vs prod.

Différence entre rôles :

- **Owner / propriétaire de santral** : supervise l'entreprise, les vendeurs,
  terminaux, limites, commissions et activité.
- **Admin** : configure et exécute les opérations quotidiennes autorisées.
- **Super Admin** : support plateforme et opérations cross-tenant.
- **Vendeur / POS** : ventes, tickets, impression et consultation.

### Build technique/interne

But : garder le savoir équipe sans l'exposer aux testeurs.

Contenu attendu :

- Architecture et sécurité ;
- flows internes ;
- docs backend/web/mobile/infra synchronisées ;
- OpenSpec et ADR ;
- conventions ;
- runbooks techniques ;
- matrices générées.

`ARCHITECTURE.md` reste la source structurelle normative pour les frontières,
règles de dépendance et placements internes. Les `PLAYBOOK` et conventions
restent des références d'implémentation internes ; ils ne doivent pas être
publiés comme guides testeurs.

### Publication

Le site public/testeurs doit pouvoir être construit en statique par MkDocs puis
publié manuellement sur Cloudflare Pages.

Exemple de cible :

- projet Cloudflare Pages `tchalanet-docs-public` ;
- domaine `docs.tchalanet.com` ;
- build public depuis `mkdocs.public.yml` ;
- output `tchalanet-docs/site-public` ;
- déploiement manuel via GitHub Actions ;
- pas de secrets dans le contenu publié.

Pour la partie technique :

- soit pas de déploiement au week-end ;
- soit sous-domaine séparé (`docs-internal.tchalanet.com`) ;
- soit chemin protégé (`docs.tchalanet.com/internal`) ;
- avec Cloudflare Access si exposé publiquement.

---

## Out Of Scope For Weekend

- SSO complet dans MkDocs lui-même.
- Gestion fine des permissions page par page dans MkDocs.
- Réécriture exhaustive de toute la documentation technique.
- Documentation iOS.
- Multi-lang complet ; le français est prioritaire pour la livraison.

---

## Decisions

- Le contenu testeur/public doit être séparé par **build** ou par **config
  MkDocs dédiée**, pas seulement caché dans la navigation.
- Pour le week-end, le choix préféré est `mkdocs.public.yml` avec
  `docs_dir: docs-public` et `mkdocs.internal.yml` avec `docs_dir: docs`.
- La documentation technique ne doit pas être publiée par accident sur
  `docs.tchalanet.com`.
- Le build public doit avoir un test anti-fuite automatisé sur HTML, sitemap et
  search index, idéalement par allowlist de pages publiques et denylist de
  termes/chemins internes.
- Cloudflare Access est le mécanisme recommandé si une surface technique doit
  être exposée hors VPN/réseau privé.
- Les parcours par rôle sont des pages guides, pas des dumps d'architecture.
- Priorité week-end : `docs-public/`, homepage, guide Admin, guide POS,
  scénarios de validation, signalement de problème, build strict et
  déploiement manuel Cloudflare Pages.

---

## Impact

- `tchalanet-docs/mkdocs.public.yml` et `tchalanet-docs/mkdocs.internal.yml`.
- `tchalanet-docs/docs-public/` :
  création d'un espace public/testeurs et de pages par rôle.
- `tchalanet-docs/docs/` :
  conservation de l'espace technique/interne.
- `tchalanet-infra/` :
  ajout d'un runbook Cloudflare Pages pour `docs.tchalanet.com`.
- CI/CD :
  build MkDocs public, build image, déploiement manuel ou staging/prod docs.
