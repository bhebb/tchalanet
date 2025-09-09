# Documentation Technique de Référence : Architecture Frontend

**Objectif** : Définir l'architecture de référence pour l'application frontend Tchalanet. Cette architecture est conçue pour être évolutive, sécurisée, multi-tenant, et pilotée par la configuration.

## 1. Principes Architecturaux Clés

- **Config-Driven UI** : L'interface est dynamiquement construite à partir de configurations JSON.
  - **Configuration Globale** : Une configuration unique, chargée au démarrage, contrôle l'affichage des sections de la page d'accueil publique.
  - **Configuration par Tenant** : Une configuration spécifique à chaque tenant (chargée après identification) contrôle le layout, le thème et les fonctionnalités de l'espace privé.
- **Tenant-First** : L'identification du tenant (via sous-domaine) est une étape prioritaire de l'initialisation de l'espace privé.
- **Permissions Fines (RBAC)** : L'affichage des fonctionnalités est contrôlé par un `PermissionService` qui vérifie la souscription du tenant, le rôle de l'utilisateur (via Keycloak), et les permissions fines accordées par l'admin du tenant.
- **i18n à Plusieurs Niveaux** : Le système de traduction (`ngx-translate`) fusionne les traductions de base avec les surcharges spécifiques au tenant.

## 2. Stack UI Recommandée

Pour garantir la cohérence, la performance et la maintenabilité, la stack UI suivante est adoptée :

- **CSS Framework** : **Tailwind CSS**. L'approche "utility-first" sera utilisée pour tous les styles.
- **Librairie de Composants** : **DaisyUI**. Utilisée pour les composants UI de base (boutons, formulaires, cards, etc.), car elle est bâtie sur Tailwind.
- **Composants Complexes** : Des librairies "headless" (comme **TanStack Table** pour les tables de données) seront privilégiées pour la logique métier, tandis que leur apparence sera entièrement gérée avec Tailwind/DaisyUI.

*Note : L'utilisation de librairies basées sur Angular Material (comme ng-matero) est proscrite pour éviter les conflits de style et la duplication des systèmes de theming.*

## 3. Schémas d'Architecture

### Schéma 1 : Flux d'Initialisation de la Page Publique

```mermaid
sequenceDiagram
    participant User
    participant AngularApp as Application Angular
    participant GlobalConfigService
    participant BackendAPI as API Tchalanet

    User->>AngularApp: Accède à tchalanet.com
    AngularApp->>GlobalConfigService: 1. (via APP_INITIALIZER) Charge la config globale
    GlobalConfigService->>BackendAPI: 2. GET /api/config/global
    BackendAPI-->>GlobalConfigService: Retourne config JSON
    GlobalConfigService-->>AngularApp: 3. Initialisation terminée
    AngularApp-->>User: 4. Affiche la page publique (avec sections conditionnelles)
```

### Schéma 2 : Flux d'Initialisation de l'Espace Privé (Utilisateur Connecté)

```mermaid
sequenceDiagram
    participant User
    participant AngularApp as Application Angular
    participant TenantServices as Services Tenant (Config, i18n)
    participant Keycloak
    participant PermissionService
    participant BackendAPI as API Tchalanet

    User->>AngularApp: Accède à tenant-a.tchalanet.com
    AngularApp->>TenantServices: 1. Identifie "tenant-a"
    TenantServices->>BackendAPI: 2. Charge config & souscriptions du tenant
    BackendAPI-->>TenantServices: Retourne config JSON
    AngularApp->>Keycloak: 3. Initialise (check-sso)
    Keycloak-->>AngularApp: 4. Retourne un JWT valide (contient les rôles)
    AngularApp->>PermissionService: 5. Charge les permissions
    PermissionService->>BackendAPI: 6. GET /api/permissions/me
    BackendAPI-->>PermissionService: Retourne la liste des permissions fines
    PermissionService-->>AngularApp: 7. Initialisation terminée
    AngularApp-->>User: Affiche l'application privée et personnalisée
```

## 4. Conception Détaillée

### 4.1. Page d'Accueil Publique

- **`GlobalConfigService`** : Un service, initialisé via `APP_INITIALIZER`, est responsable de l'unique appel `GET /api/config/global`. Il stocke la configuration dans un `Signal` ou un `BehaviorSubject` pour un accès réactif.
- **`HomePublicComponent`** : Ce composant injecte `GlobalConfigService`. Son template HTML utilise des `*ngIf` pour afficher ou masquer les composants enfants (`HeroSectionComponent`, `FeaturesSectionComponent`, etc.) en fonction de la configuration chargée.
- **Contenu** : Tous les textes sont gérés par `ngx-translate` avec des fichiers JSON locaux (`/assets/i18n/*.json`).

### 4.2. Espace Privé

- **`TenantService` & `TenantConfigService`** : Identifient le tenant et chargent sa configuration spécifique (thème, layout de la homepage privée, langues, souscriptions).
- **`PermissionService`** : Le service central pour la logique d'autorisation. Il expose une méthode `hasPermission(permission: string, subscription?: string): boolean` qui sera utilisée dans toute l'application (via des `*ngIf` ou des `Guards`).
- **Architecture de Rendu Dynamique** : La page d'accueil privée utilise un `HomeContainerComponent` qui lit la configuration du layout du tenant et rend dynamiquement une série de "widgets" (`app-widget-*`), chacun vérifiant ses propres permissions avant de s'afficher.

## Stratégie CDN et Caching — Home Public vs Home Privé

### Décision
- **Cloudflare CDN** est utilisé **uniquement pour les assets statiques** : images (hero, news), logos, polices, fichiers `.webp/.png/.svg`, éventuellement bundles versionnés.
- **Tous les appels de service** (configs, i18n overrides, rules, lotteries, news, services privés) passent **directement par l’Origin** (API), **sans CDN**.

---

### Home Public (non connecté)
**Flow**
1. Bootstrap (détection langue, lecture toggles).
2. `GET /configs/home?ctx=default` (Origin) → applique **theme**, **header**, **sections**.
3. `GET /rules?ctx=default` (Origin) → **evaluate**(sections, toggles, contexte) → sections filtrées/props ajustées.
4. Données widgets visibles :
    - Loteries : `GET /lotteries?src=...` (Origin)
    - News : `GET /news?limit=...&lang=...` (Origin)
    - i18n overrides : `GET /configs/i18n/default?lang=xx` (Origin) → merge **base + overrides**
5. **Assets** référencés par les widgets (images, logos) : chargés via **Asset CDN**.
6. Rendu Home (skeletons + SWR côté app si besoin).

**Caching**
- **Backend** : Redis + in-memory (Caffeine) pour `configs/home`, `rules`, `i18n` (overrides), avec **ETag/Version** et SWR.
- **Client** : memo par session + ETag/If-None-Match.
- **CDN** : non appliqué aux services; **oui uniquement pour /assets/**.

**Headers (services)**
-- Cache-Control: public, max-age=300, stale-while-revalidate=3600
-- ETag: "W/<sha256>"
-- Content-Type: application/json
-- Vary: Accept-Language


---

### Home Privé (connecté)
**Flow**
1. OIDC (Keycloak) → tokens.
2. Tenant-first (subdomaine/mapping).
3. `GET /configs/home?ctx=tenant:<code>` (Origin) → sections + `visibleIf`.
4. i18n : `base → default → tenant` via Origin (`/configs/i18n/tenant/<code>?lang=xx`).
5. RBAC : `PermissionService` (rôles, souscriptions, permissions fines).
6. Règles : `GET /rules?ctx=tenant:<code>` (Origin) → **evaluate**(sections, claims, toggles).
7. Données privées par widget **origin only** (souvent `no-store` si sensible).
8. **Assets** branding/illustrations via **Asset CDN**.
9. Rendu Dashboard (web/mobile), vues distinctes (Ionic côté mobile).

**Caching**
- **Backend** : Redis + in-memory pour `configs/home`, `rules`, `i18n tenant`.
- **Pas de cache partagé** pour réponses **per-user**. Utiliser **no-store** si nécessaire.
- **CDN** : jamais pour les endpoints privés; **oui** pour assets statiques (logos/avatars si publics).

**Sécurité**
- Clés de cache incluant `{tenant}`, `{lang}`, `{version}`.
- Éviter de mettre des données sensibles dans des caches partagés.
- Headers possibles côté privé : `Cache-Control: no-store` selon la sensibilité.

---

### Règles & Évaluation
- **Ruleset** chargé depuis Origin; résultats d’évaluation **non** mis en cache global (dépendent du contexte utilisateur).
- Contexte rules : toggles, environnement (heure, geo si autorisée), claims RBAC, paramètres de sections.
- En cas d’erreur rules/configs/i18n → fallback **bundle** (base) ou **stale** (mémoire/Redis) avec bandeau “mode dégradé”.

---

### Observabilité
- Métriques : hit ratio (in-mem/Redis), latence P95, taux de stale, taux d’erreurs Origin.
- Alerte si : hit ratio chute, stale > seuil, Origin en erreur.
- Warm-up post-deploy : prime caches `configs/rules/i18n` pour tenants actifs.

---

### Résumé
- **Services = Origin only** (sécurité, cohérence), **Assets = CDN** (perf/UI).
- **Caches backend** (Redis + in-mem) + **ETag/SWR** => latence basse sans CDN services.
- **Règles & RBAC** au cœur du filtrage des widgets; **vues distinctes** web vs mobile (Ionic).
