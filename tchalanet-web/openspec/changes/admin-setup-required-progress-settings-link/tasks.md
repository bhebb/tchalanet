# Tasks — admin-setup-required-progress-settings-link

> Lire ce fichier en premier au début de chaque session.
> Cocher en temps réel (`[ ]` → `[x]`) dès qu'une tâche est terminée.

---

## W1 — Progression required-only

- [x] **W1.1** Dans `admin-complete-tenant-config.page.ts`, ajouter la constante **hors classe** (module-level) :
  ```ts
  const REQUIRED_SETUP_SECTION_IDS = ['identity', 'address', 'games_pricing', 'draws'] as const;
  ```
  Puis remplacer `progressPct` / les références à `setup().totalSteps` / `setup().completedSteps` par des fields qui la référencent directement :
  ```ts
  readonly requiredTotalCount = REQUIRED_SETUP_SECTION_IDS.length; // 4 — pas de signal, valeur stable
  readonly requiredCompletedCount = computed(() =>
    REQUIRED_SETUP_SECTION_IDS.filter(id => this.sectionMap().get(id)?.status === 'READY').length
  );
  readonly progressPct = computed(() =>
    Math.round((this.requiredCompletedCount() / this.requiredTotalCount) * 100)
  );
  ```
- [x] **W1.2** Dans `admin-complete-tenant-config.page.html`, mettre à jour le binding du titre de progression :
  ```html
  {{ 'admin.setup.progress.title' | translate : { completed: requiredCompletedCount(), total: requiredTotalCount } }}
  ```
- [x] **W1.3** Mettre à jour la clé `admin.setup.progress.title` dans `fr.json`, `en.json`, `ht.json` : `"{{completed}} / {{total}} étapes requises complétées"` (vérifier qu'elle n'est pas utilisée ailleurs avant de modifier).

---

## W2 — Box « Paramètres avancés » (inline)

- [x] **W2.1** Dans `admin-complete-tenant-config.page.html`, ajouter entre la fermeture de `.setup__grid` et le bloc `.setup__card--terminal` :
  ```html
  <div class="setup__settings-link">
    <mat-icon class="setup__settings-link__icon">settings</mat-icon>
    <div class="setup__settings-link__body">
      <span class="setup__settings-link__title">
        {{ 'admin.setup.advancedSettings.title' | translate }}
      </span>
      <span class="setup__settings-link__desc">
        {{ 'admin.setup.advancedSettings.description' | translate }}
      </span>
    </div>
    <a mat-stroked-button routerLink="/app/admin/settings/config">
      {{ 'admin.setup.advancedSettings.action' | translate }}
    </a>
  </div>
  ```
- [x] **W2.2** Dans `admin-complete-tenant-config.page.scss`, ajouter `@use '@tch/ui/styles' as ui;` si absent, puis le bloc `.setup__settings-link` — **mobile-first, colonne par défaut, row à partir de `medium`** :
  ```scss
  .setup__settings-link {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
    padding: 1rem 1.25rem;
    border: 1px solid var(--tch-color-outline-variant);
    border-radius: var(--tch-radius-lg);
    background: var(--tch-color-surface-container-low);

    @include ui.up(medium) {
      flex-direction: row;
      align-items: center;
    }
  }

  .setup__settings-link__icon {
    color: var(--tch-color-on-surface-variant);
    font-size: 1.5rem;
    width: 1.5rem;
    height: 1.5rem;
    flex-shrink: 0;
  }

  .setup__settings-link__body {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    flex: 1;
  }

  .setup__settings-link__title {
    font-weight: 600;
    font-size: 0.9375rem;
    color: var(--tch-color-on-surface);
  }

  .setup__settings-link__desc {
    font-size: 0.875rem;
    color: var(--tch-color-on-surface-variant);
  }
  ```
  Aucun `max-width` ni media query custom — uniquement `@include ui.up(medium)` (≥ 600px).

---

## W3 — I18n

Les fichiers i18n sont du JSON **imbriqué**. Les clés à ajouter doivent respecter la structure existante — ne pas aplatir.

- [x] **W3.1** Dans `fr.json`, sous `admin > setup`, ajouter la clé `advancedSettings` et modifier `progress.title` :
  ```json
  "admin": {
    "setup": {
      "progress": {
        "title": "{{completed}} / {{total}} étapes requises complétées"
      },
      "advancedSettings": {
        "title": "Paramètres avancés",
        "description": "Langue, reçu, communication et règles métier peuvent être ajustés dans les paramètres du tenant.",
        "action": "Ouvrir les paramètres"
      }
    }
  }
  ```
  Et sous `common`, ajouter `"refresh": "Actualiser"`.
  > `nav.admin.settings` existe déjà en fr.json (`"Paramètres"`) — ne pas dupliquer.

- [x] **W3.2** Dans `en.json`, mêmes emplacements imbriqués :
  ```json
  "admin": {
    "setup": {
      "progress": {
        "title": "{{completed}} / {{total}} required steps completed"
      },
      "advancedSettings": {
        "title": "Advanced settings",
        "description": "Language, receipt, communication and business rules can be adjusted in tenant settings.",
        "action": "Open settings"
      }
    }
  }
  ```
  Et `"common": { "refresh": "Refresh" }`.
  Vérifier si `nav.admin.settings` existe déjà en en.json avant d'ajouter.

- [x] **W3.3** Dans `ht.json`, mêmes emplacements imbriqués :
  ```json
  "admin": {
    "setup": {
      "progress": {
        "title": "{{completed}} / {{total}} etap obligatwa konplète"
      },
      "advancedSettings": {
        "title": "Paramèt avanse",
        "description": "Lang, resi, kominikasyon ak règ biznis ka ajiste nan paramèt tenant lan.",
        "action": "Ouvri paramèt"
      }
    }
  }
  ```
  Et `"common": { "refresh": "Aktyalize" }`.
  Vérifier si `nav.admin.settings` existe déjà en ht.json avant d'ajouter.

---

## W4 — Sidenav

- [x] **W4.1** Dans `private-navigation.model.ts`, dans le groupe `more`, ajouter avant ou après `mySpace` :
  ```ts
  {
    id: 'adminSettings',
    labelKey: 'nav.admin.settings',
    icon: 'settings',
    destination: { kind: 'route', value: '/app/admin/settings' },
  },
  ```
- [x] **W4.2** `nav.admin.settings` existe déjà dans `fr.json` (`"Paramètres"`). Vérifier et ajouter uniquement dans `en.json` et `ht.json` si absent.

---

## Validation finale

- [x] **V1** Le compteur affiche `X / 4 étapes requises complétées` (pas 6).
- [x] **V2** `canCreateSellerTerminal=true` quand les 4 sections requises sont READY (comportement existant inchangé).
- [x] **V3** La box Paramètres avancés est visible entre le grid et le bloc Seller-terminal.
- [x] **V4** Le lien pointe vers `/app/admin/settings/config`.
- [x] **V5** La box n'affecte pas le compteur de progression.
- [x] **V6** Le bouton Actualiser affiche "Actualiser" (fr) et non la clé brute.
- [x] **V7** `Paramètres` apparaît sous `Plus` dans la sidenav admin.
- [x] **V8** Layout responsive : la box collapse en colonne sur mobile (< 600px).
- [x] **V9** Aucune couleur hardcodée dans le SCSS ajouté — uniquement `--tch-*`.
- [x] **V10** `pnpm nx lint tch-portal` et `pnpm nx build tch-portal` passent sans erreur.
