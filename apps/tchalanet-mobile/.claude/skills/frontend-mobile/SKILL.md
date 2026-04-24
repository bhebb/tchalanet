---
name: frontend-mobile
description: >
  Use when writing or reviewing code in apps/tchalanet-mobile — enforces Ionic 8 + Capacitor 7 patterns, prohibits Cordova usage, covers shared library usage, tabs-based navigation, and offline sync integration.
---

# Mobile — Conventions Ionic 8 / Capacitor 7

> Source de vérité : `openspec/context/40-mobile-rules.md`

## Stack

- Ionic **8.7.x** — framework UI mobile
- Capacitor **7.4.x** — runtime natif (iOS + Android)
- Angular (partagé avec web)
- pnpm + Nx (même monorepo)

---

## Règles fondamentales

- **Capacitor uniquement** pour les APIs natives — ❌ jamais Cordova
- Pas de plugins Cordova dépréciés
- APIs natives : `Camera`, `Filesystem`, `Preferences` (Capacitor plugins)

---

## Architecture

```
apps/tchalanet-mobile/
├─ src/
│  ├─ app/
│  │  ├─ tabs/         ← navigation tabs-based
│  │  │  ├─ tab1/
│  │  │  ├─ tab2/
│  │  │  └─ tab3/
│  │  └─ app.routes.ts
│  └─ environments/
└─ capacitor.config.ts
```

### Partage de libs avec le web

```typescript
// Les libs partagées sont accessibles directement
import { AuthService } from '@tchalanet/shared/auth';
import { SessionFacade } from '@tchalanet/shared/facades';

// Libs partagées :
// @tchalanet/shared/* (auth, api, config, types, facades, data-access, utils)
// @tchalanet/i18n/*   (traductions)
```

---

## Navigation

- Structure **tabs-based** (Ionic tabs)
- Routes enregistrées dans `app.routes.ts`
- Lazy loading des modules de tabs

---

## Offline sync

- Fonctionnalité gérée côté serveur via `core/offlinesync/`
- Le mobile déclenche la synchronisation au retour de connectivité
- Préférences locales via `Preferences` (Capacitor Storage)

---

## Composants Ionic

```typescript
// Composants Ionic standalone
import { IonHeader, IonContent, IonList, IonItem } from '@ionic/angular/standalone';

@Component({
  standalone: true,
  imports: [IonHeader, IonContent, IonList, IonItem, TranslateModule],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ 'tabs.tab1.title' | translate }}</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content>
      @for (item of items(); track item.id) {
        <ion-item>{{ item.name }}</ion-item>
      }
    </ion-content>
  `
})
```

---

## Build et déploiement

```bash
# Build web assets
nx build tchalanet-mobile

# Sync avec Capacitor
npx cap sync

# iOS
npx cap open ios

# Android
npx cap open android
```

---

## Checklist nouveau composant mobile

- [ ] Ionic components importés en standalone (pas depuis `@ionic/angular` module)
- [ ] APIs natives via `@capacitor/*` uniquement
- [ ] Strings via i18n (mêmes locales : fr / en / ht)
- [ ] Pas de Cordova plugins
- [ ] Libs partagées depuis `@tchalanet/shared/*`
