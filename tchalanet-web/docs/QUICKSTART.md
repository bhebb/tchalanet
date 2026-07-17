# Tchalanet Web — Quickstart

> **Apps** : `apps/public-portal/`, `apps/admin-portal/`, `apps/platform-portal/`
> **Stack** : Angular / Nx / SCSS / Playwright / Vitest
>
> Ce document couvre uniquement le démarrage. L'architecture, les conventions et les règles
> vivent dans [`ARCHITECTURE.md`](./ARCHITECTURE.md) et [`conventions/`](./conventions/README.md) —
> ne pas les dupliquer ici.

---

## 1. Démarrage rapide

### Installer les dépendances

```bash
pnpm install
```

### Démarrer les applications

```bash
pnpm serve:portals
```

URLs locales:

- Public: http://localhost:4301
- Admin: http://localhost:4302
- Platform: http://localhost:4303

Variantes:

```bash
pnpm serve:portals:emulator
pnpm serve:portals:docker
pnpm serve:portals:docker-emulator
pnpm serve:portals:stg
pnpm serve:portals:prod
```

Chaque app peut aussi être lancée seule via `pnpm serve:public-portal`,
`pnpm serve:admin-portal` ou `pnpm serve:platform-portal`, mais les raccourcis
recommandés gardent les ports standards:

```bash
pnpm serve:public
pnpm serve:admin
pnpm serve:platform
pnpm serve:admin:docker-emulator
```

Pour lancer seulement un sous-ensemble:

```bash
pnpm serve:portals -- --only=admin,platform
pnpm serve:portals:docker-emulator -- --only=admin
```

### Lancer les tests unitaires

```bash
pnpm test
```

### Lancer les tests end-to-end

```bash
make -C ../tchalanet-infra up-firebase-emulator
pnpm e2e:web
```

`web-e2e` est le projet Playwright unique. Les tests sont rangés par surface sous
`apps/web-e2e/src/{public-portal,admin-portal,platform-portal}`.

Raccourcis utiles:

```bash
pnpm e2e:web:admin
pnpm e2e:web:admin-business
pnpm e2e:web:api
```

---

## 2. Création initiale du workspace

À utiliser seulement si le workspace n’existe pas encore.

```bash
pnpm init
npx create-nx-workspace@latest .
pnpm install
```

Installer le plugin Angular Nx si nécessaire :

```bash
pnpm add -D @nx/angular
```

Générer une nouvelle app seulement si le workspace doit ajouter une surface déployable :

```bash
pnpm nx g @nx/angular:app <app-name> \
  --directory=apps/<app-name> \
  --routing \
  --style=scss \
  --prefix=tch \
  --standalone \
  --unitTestRunner=vitest \
  --e2eTestRunner=playwright \
  --tags=scope:<surface>,type:app
```

> Ne pas utiliser cette commande dans un workspace déjà initialisé sauf décision explicite.

---

## 3. Où lire quoi

| Besoin | Document |
| ------ | -------- |
| Architecture, carte des briques, règles non négociables | [`ARCHITECTURE.md`](./ARCHITECTURE.md) |
| Créer un écran console (liste/détail/création/…) | [`conventions/feature-playbook.md`](./conventions/feature-playbook.md) |
| Où placer un fichier / une lib | [`conventions/placement-guide.md`](./conventions/placement-guide.md) |
| Nommage | [`conventions/naming.md`](./conventions/naming.md) |
| Tags et frontières Nx | [`conventions/nx-boundaries.md`](./conventions/nx-boundaries.md) |
| Index complet des conventions | [`conventions/README.md`](./conventions/README.md) |
