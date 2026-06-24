# Proposal — Admin Setup: progression requise + lien paramètres

> Status: DRAFT
> Date: 2026-06-22
> Scope: tchalanet-web — feature admin/setup uniquement

---

## Why

La page `/app/admin/setup` a trois défauts visibles après le provisioning d'un tenant :

1. **Contradiction progression** — le compteur affiche `"2 / 6 sections complétées"` parce que `totalSteps` inclut les sections optionnelles (Apparence, Maryaj Gratis). En même temps le bloc Seller-terminal dit `"Toutes les étapes requises sont complétées"`. Ces deux états sont incompatibles et déroutent l'admin.

2. **Pas d'accès aux paramètres** — `/app/admin/settings/config` (langue, reçu, communication, règles métier) est introuvable depuis la page setup. L'admin doit deviner la route ou chercher dans la sidenav, où `Paramètres` n'est pas exposé.

3. **Clé i18n manquante** — `common.refresh` n'existe pas dans `fr.json`, `en.json`, `ht.json`. Le bouton Actualiser de la page setup affiche la clé brute au lieu du libellé.

---

## What

### W1 — Progression required-only

Remplacer le calcul de progression basé sur `setup.totalSteps / setup.completedSteps` (backend, toutes sections confondues) par un calcul frontend ne comptant que les sections **requises**.

Liste statique des IDs requis (stable, définie côté frontend, alignée avec `blockingSteps` backend) :

```ts
private readonly REQUIRED_IDS = ['identity', 'address', 'games_pricing', 'draws'] as const;
```

Seller-terminal = action de démarrage finale, **hors compteur**.
Apparence et Maryaj Gratis = optionnels, **hors compteur**.

Le titre de progression devient : `"{{completed}} / {{total}} étapes requises complétées"`.

### W2 — Box « Paramètres avancés » (inline)

Ajouter entre le grid de cards et le bloc Seller-terminal une zone discrète de navigation vers `/app/admin/settings/config`.

**Composant** : inline dans `admin-complete-tenant-config.page.html` (pas de nouveau composant séparé — la zone est < 10 lignes HTML).

**Layout** : flex row — icône + bloc texte + bouton `mat-stroked-button`, pleine largeur (`grid-column: 1 / -1`).

**Styling** : utiliser les tokens `--tch-*` canoniques du projet, pattern BEM `.setup__settings-link` aligné sur le bloc `.setup__progress-block` existant :
- fond : `var(--tch-color-surface-container-low)`
- bordure : `1px solid var(--tch-color-outline-variant)`
- radius : `var(--tch-radius-lg)`
- icône : `var(--tch-color-on-surface-variant)` (discrète, pas primary ni accent)
- texte : `var(--tch-color-on-surface-variant)` pour la description
- bouton : `mat-stroked-button` standard (pas flat, pas accent — action secondaire)

**Ne pas utiliser `TchNotice type="info"`** — ce composant est réservé aux messages système/alertes, pas aux shortcut de navigation. Il rendrait la box trop saillante (border-left primary bleue).

**Ne pas utiliser `TchCard`** — la card est pour les widgets de contenu. La box settings est un bloc de navigation compact.

Comportement :
- toujours visible sur `/app/admin/setup`
- ne compte pas dans la progression
- ne bloque pas la création du Seller-terminal
- responsive mobile : passe en colonne sous `compact` (< 600px)

### W3 — I18n (fr / en / ht)

Nouvelles clés à ajouter dans les trois fichiers :

```json
"admin.setup.advancedSettings.title": "Paramètres avancés",
"admin.setup.advancedSettings.description": "Langue, reçu, communication et règles métier peuvent être ajustés dans les paramètres du tenant.",
"admin.setup.advancedSettings.action": "Ouvrir les paramètres",
"common.refresh": "Actualiser"
```

Clé existante à **modifier** (changement de sens) :

```json
"admin.setup.progress.title": "{{completed}} / {{total}} étapes requises complétées"
```

> Note : ancienne valeur = `"{{completed}} / {{total}} sections complétées"`. Vérifier qu'elle n'est pas référencée ailleurs avant de modifier.

### W4 — Sidenav : exposer Paramètres sous `more`

Dans `TENANT_ADMIN_NAVIGATION` (`private-navigation.model.ts`), ajouter dans le groupe `more` :

```ts
{
  id: 'adminSettings',
  labelKey: 'nav.admin.settings',
  icon: 'settings',
  destination: { kind: 'route', value: '/app/admin/settings' },
},
```

Nouvelle clé i18n (fr/en/ht) : `"nav.admin.settings": "Paramètres"`.

---

## Impact

### Fichiers modifiés

| Fichier | Nature |
|---|---|
| `pages/complete-config/admin-complete-tenant-config.page.ts` | Computed `requiredCompletedCount`, `requiredTotalCount`, `progressPct` |
| `pages/complete-config/admin-complete-tenant-config.page.html` | Box settings link + titre progression |
| `pages/complete-config/admin-complete-tenant-config.page.scss` | `.setup__settings-link` + responsive |
| `shell/private-navigation.model.ts` | Ajout `adminSettings` sous `more` |
| `assets/i18n/fr.json` | 4 nouvelles clés + 1 clé modifiée |
| `assets/i18n/en.json` | Idem |
| `assets/i18n/ht.json` | Idem |

### Pas de changement backend

`canCreateSellerTerminal` et `blockingSteps` sont déjà corrects côté API. Pas de nouveau endpoint, pas de nouveau modèle.

---

## Non-goals

- Pas de nouveau composant Angular standalone (box inline dans la page).
- Pas de changement sur `admin-settings.page` ni `admin-config.page`.
- Pas de deep links `?tab=` vers les tabs settings (hors scope V0).
- Pas de modification du routage existant.
- Pas de remplacement de `mySpace` dans la sidenav — il reste pointé sur `/app/admin/setup`.

---

## Style rules applicables

- BEM : `.setup__settings-link`, `.setup__settings-link__icon`, `.setup__settings-link__body`, `.setup__settings-link__title`, `.setup__settings-link__desc`.
- Tokens uniquement (`--tch-*`). Aucune couleur hardcodée.
- Responsive via `@use '@tch/ui/styles' as ui` + `@include ui.up(medium)` (≥ 600px). Mobile-first.
- `--comp-*` non requis ici : la box est feature-local, pas un composant réutilisable.
- Pas de `::ng-deep`, pas de z-index custom.

---

## Open questions

1. **Liste statique vs `blockingSteps`** — La liste `['identity', 'address', 'games_pricing', 'draws']` est hardcodée frontend. Si le backend ajoute une nouvelle section blocking, les deux doivent être mis à jour de concert. Alternative future : ajouter `required: boolean` sur `ReadinessSection` (backend change hors scope).

2. **Seller-terminal dans le total** — Décision actuelle : hors compteur (action finale). Valider avec les critères d'acceptation que `canCreateSellerTerminal=true` avec 4/4 étapes requises est cohérent (et non 5/5).

3. **`mySpace` redondant** — L'item `mySpace` de la sidenav redirige vers `/app/admin/setup`, qui est déjà l'item `setup` en top-level. Hors scope de ce change mais à surveiller pour un nettoyage.
