# OpenSpec Change — i18n Trilingual Editorial Pass V1

## Status

Proposed — 2026-08-05

## Why

Le corpus de traduction n'a jamais été relu par un traducteur. Il a été **construit par
renommages successifs en search-and-replace**, chaque locale figée à une génération différente du
modèle de domaine. L'audit du corpus (4 048 clés web × 3 locales + 429 clés mobile × 3) montre
quatre défauts structurels.

### 1. L'identifiant de code fuit dans la copy utilisateur

Le renommage `Terminal` + `SalesSession` + `Seller` → `SellerTerminal` (décision d'architecture,
voir `docs/00-guidelines/glossary.md`) a été appliqué **aux fichiers de traduction comme au code**.
Le nom d'entité technique se lit donc tel quel sur le site public : **80 chaînes FR, 78 chaînes EN.**

```
public.manager.description   « Pour les seller-terminals, seller-terminals et gestionnaires de réseau. »
public.operator.faq_…_q      « Combien de seller-terminals ou seller-terminals puis-je utiliser ? »
public.contact_cta.body      « Expliquez-nous votre seller-terminal ou votre réseau. »
```

Le doublon de mot est la signature du `sed` : deux termes sources distincts ont été réécrits vers
la même cible. Aucune relecture n'a suivi.

### 2. Trois noms concurrents pour un même acteur, dans la même locale

Le FR emploie simultanément `vendeur` (144 occurrences), `terminal` (128) et `seller-terminal` (84)
pour désigner le SellerTerminal. Idem pour l'organisation cliente : `tenant` (204, dont **22 dans
`errors.json` — donc lues par un vendeur en caisse**), `espace` (39), `opérateur` (10).

### 3. Les trois locales ne décrivent pas le même produit

| | clés | manquantes /FR | orphelines | FR résiduel |
|---|---|---|---|---|
| FR (référence) | 4 048 | — | — | — |
| EN | 4 028 | **88** | **68** | **126** |
| HT | 4 102 | 0 | **54** | **29** |

Les orphelines diffèrent par locale : le HT conserve l'ancien modèle (`cashier.session.*`,
`nav.terminals`, `admin.seller.outletId`), l'EN une génération intermédiaire
(`cashier.connection.*`, `nav.connections`). Chaque locale est un fossile d'un refactor distinct.

### 4. Orthographe kreyòl inconstante

`ankò` s'écrit `anko` dans 15 chaînes (contre 113 correctes), `pwoblèm` → `pwoblem` (2),
`sèvis` → `sevis` (1). Concentré dans `errors.json` : ce fichier a été traduit par une autre main.

### Ce que l'audit établit aussi

- **0 mismatch de placeholders**, **0 valeur vide** sur ~12 000 chaînes. La mécanique i18n est saine.
- Le **HT est la locale la mieux traduite**, pas la plus faible : il localise déjà `tenant` → `santral`
  (173) / `espas` (70) selon le registre, et n'a qu'une seule fuite technique. C'est l'**EN qui est
  le maillon faible**, et le **FR qui est contaminé**.
- Le mobile est sain : parité parfaite, 5 chaînes à corriger.

## Decision (locked)

- **Deux registres, explicites.** Un terme *domaine* (code, API, docs techniques) et un terme
  *public* (UI, docs publiques) par concept. `SellerTerminal` est un nom de classe, jamais un mot
  d'interface. La règle existait déjà — `docs-public/glossaire/index.md` — elle n'était pas appliquée.
- **Terme public de l'acteur de vente : « Vendeur » / « Seller » / « Vandè ».** Choix aligné sur
  l'usage majoritaire déjà présent en FR (144 occurrences) et sur le glossaire public.
- **Le FR reste la langue source.** EN et HT en sont dérivés, pas l'inverse.
- **Le HT suit l'orthographe IPN.** `apre` sans accent est correct ; `ankò`, `pwoblèm`, `sèvis` en
  portent un. Pas de re-francisation.
- **Le glossaire trilingue devient la source de vérité**, versionné dans `tchalanet-docs`.
- Registre : **vouvoiement en FR**, impératif direct court en EN, `ou` en HT. Pas d'exclamation.

## What Changes

- Un **glossaire trilingue FR/EN/HT** (`docs-public/glossaire/index.md`), aligné sur le modèle
  SellerTerminal et sur les deux registres.
- **Passe éditoriale complète** sur les 13 namespaces web × 3 locales, namespace par namespace :
  terminologie, registre, ton, longueur UI, cohérence intra-locale.
- **Purge des 122 clés orphelines** (68 EN + 54 HT) et **complétion des 88 clés EN manquantes**.
- **Passe mobile** (5 chaînes + vérification terminologique).

## Impact

- `tchalanet-web/libs/shared-assets/public/assets/i18n/{fr,en,ht}/*.json` — 13 namespaces.
- `tchalanet-mobile/assets/i18n/{fr,en,ht}/*.json` — 12 namespaces.
- `tchalanet-docs/docs-public/glossaire/index.md`.
- **Aucun changement de clé consommée par le code** hors suppression d'orphelines vérifiées
  non référencées. Aucun changement de composant, de route ou de contrat d'API.

## Non-goals

- Pas de refonte du mécanisme i18n (`@tch/core/i18n`), du merger ni du loader.
- Pas de renommage de clés existantes encore consommées.
- Pas de nouvelle langue.
- Pas de garde-fou CI dans ce change (parité + interdiction d'identifiants techniques) — noté
  comme suite recommandée, hors périmètre décidé.
- Pas de modification du glossaire **technique** (`docs/00-guidelines/glossary.md`) : il est correct,
  c'est son application à l'UI qui était fautive.
