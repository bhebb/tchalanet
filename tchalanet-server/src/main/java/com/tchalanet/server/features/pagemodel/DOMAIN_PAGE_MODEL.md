# Domaine pagemodel

> Domaine responsable de la description, la personnalisation et la résolution des pages (publiques, privées, admin) à partir de modèles JSON stockés en base.

---

## 1. Rôle du domaine

**Responsabilité principale**

Centraliser la **configuration des pages** Tchalanet (public/privé/admin) sous forme de modèles JSON (`PageModel`) stockés en base, et fournir au BFF une vue normalisée de ces modèles, enrichie avec la **donnée dynamique** adaptée au tenant, au rôle et à la langue.

**Ce que fait le domaine**

- Décrit la structure des pages (meta, thème, shell, layout, widgets) via un `PageModel`.
- Gère des **templates de page** globaux (`PageTemplate`) qui servent de base pour les tenants.
- Gère des **instances de page par tenant** (`PageModel`), incluant : statuts (DRAFT, PUBLISHED, ARCHIVED) et mapping tenant + rôle + identifiant logique de page.
- Résout, pour une requête donnée :
  - quel modèle utiliser (template vs page tenant)
  - dans quelle langue (`currentLang`)
  - quelles sources dynamiques doivent être appelées.
- Expose au BFF une `PageModelResponse` qui contient :
  - `currentLang`, `langs`
  - le `pageModel` normalisé
  - un bloc `dynamic` (données métiers)
  - des informations i18n (`namespaces`, `overrides`)
  - des `issues` (diagnostics de configuration).

Le domaine **pagemodel** n’effectue pas de calculs métiers (KPIs) : il orchestre des providers dynamiques situés dans d’autres features (`pagemodel.shared.dynamic`, `private_dashboard.dynamic`, etc.).

---

## 2. Cas d’utilisation principaux

### Public

- Afficher la page publique par défaut (ex : `public.home`) sans tenant (page Tchalanet globale).
- Afficher la page publique propre à un tenant si une page spécifique a été configurée.
- Renvoyer, pour la home publique : tirages et résultats récents, plans B2B, actualités, sections statiques (hero, features, check ticket…).

### Privé

- Afficher le dashboard privé adapté au rôle :
  - `SUPER_ADMIN` → vue plateforme
  - `TENANT_ADMIN` → vue tenant
  - `CASHIER` → vue vendeur POS
  - `OPERATOR` → à gérer plus tard
- Résoudre, à partir du `PageModel`, quelles sections doivent être alimentées depuis : `reporting/*`, `draw`, `session`, `autonomy`, `audit`, etc.
- Permettre l’évolution de la structure des dashboards via la configuration des widgets/layout sans changer le code.

### Admin / Template

- Permettre au **super admin** de gérer les `PageTemplate` (par scope/role).
- Permettre au **tenant admin** de cloner un template, modifier en DRAFT, publier (PUBLISHED), dupliquer/archiver.
- Bootstrap d’un nouveau tenant : cloner les templates par défaut en `PageModel` pour ce tenant.

---

## 3. Frontières & dépendances

**Dépendances du domaine pagemodel**

- `accesscontrol` (résolution de `TchRole`)
- `tenantconfig` (locale par défaut et paramètres)
- `identity` / `user_preference` (préférences locales)
- `reporting/*` (`salesreport`, `outletperformance`, `tenantkpis`, `tenant_dashboard`)
- `draw` (tirages, résultats)
- `news`, `plans/billing`
- `autonomy` / `validation`
- `audit`

Le domaine est consommé par le BFF / adaptateurs REST (`GET /api/public/pages/{slug}`, `GET /api/private/dashboard`, `GET /api/admin/pages/templates`).

Le domaine pagemodel doit rester un orchestrateur : pas de logique métier critique dupliquée ici.

---

## 4. Modèle conceptuel

### 4.1 PageTemplate (concept)

Champs clés : `id`, `logical_page_id`, `scope`, `role?`, `schema_version`, `page_model_json`, `is_default`, métadonnées.

Seul le super admin peut gérer les `PageTemplate`.

### 4.2 PageModel (instance tenant)

Champs clés : `id`, `tenant_id` (nullable), `logical_page_id`, `scope`, `role?`, `status` (DRAFT|PUBLISHED|ARCHIVED), `template_id?`, `page_model_json`, `version`, métadonnées.

Règle importante : pour la combinaison `(tenant_id, logical_page_id, role)` il ne doit y avoir qu’un seul `PageModel` en `PUBLISHED`.

Exemple (simplifié) du champ `page_model_json` :

```json
{
  "meta": { "id": "public.home", "langs": ["fr", "en", "ht"], "default_lang": "fr" },
  "theme": { "preset": "tchalanet_default", "mode": "light", "density": 0 },
  "shell": { "header": {}, "sidenav": null, "footer": {} },
  "content": {
    "layout": { "component": "GridLayout", "rows": [] },
    "widgets": {
      "home.hero": { "type": "HeroWidget", "binding": { "mode": "static" }, "props": {} },
      "home.draws": {
        "type": "DrawsWidget",
        "binding": { "mode": "dynamic", "source": "results_by_game" },
        "props": {}
      }
    }
  }
}
```

### 4.3 Statuts et versions

- DRAFT : édition
- PUBLISHED : version active (unique par combinaison tenant/logical_page_id/role)
- ARCHIVED : historique

Workflow : cloner template → créer `PageModel` DRAFT → modifier → publier → anciennes versions archivées.

---

## 5. Persistance & tables

Tables principales :

- `page_template` : id, logical_page_id, scope, role, schema_version, page_model_json, is_default, timestamps, user meta.
- `page_model` : id, tenant_id, logical_page_id, scope, role, status, schema_version, template_id, page_model_json, version, timestamps, user meta.

Contraintes : index unique sur `(tenant_id, logical_page_id, role, status='PUBLISHED')` et RLS sur `tenant_id`.

---

## 6. API & interaction BFF

### Réponse type `PageModelResponse`

```json
{
  "currentLang": "fr",
  "langs": ["fr", "en", "ht"],

  "pageModel": {},

  "dynamic": {},

  "i18n": {
    "namespaces": [],
    "overrides": {}
  },

  "issues": []
}
```

### Résolution du PageModel

- Public : si URL spécifique à un tenant → chercher `PageModel` PUBLISHED pour ce tenant ; sinon modèle global (tenant_id = NULL).
- Privé : résoudre `tenant_id` et `TchRole`, chercher `PageModel` PUBLISHED pour `(tenant, logical_page_id, role)`, fallback sur `PageTemplate` par défaut si absent.

---

## 7. Résolution de langue & i18n

Algorithme pour `currentLang` :

1. Langue forcée par URL / paramètre.
2. Langue préférée de l’utilisateur si autorisée.
3. Langue par défaut du tenant si autorisée.
4. `meta.default_lang` si présent et autorisé.
5. Premier élément de `meta.langs`.

Pseudo‑code :

```java
String resolveCurrentLang(Request request, Tenant tenant, UserPreference userPref, PageModel pageModel) {
    var allowed = pageModel.meta().langs();
    var langFromUrl = extractLangFromUrlOrQuery(request);
    if (allowed.contains(langFromUrl)) return langFromUrl;
    if (userPref != null && allowed.contains(userPref.locale())) return userPref.locale();
    if (tenant.defaultLocale() != null && allowed.contains(tenant.defaultLocale())) return tenant.defaultLocale();
    if (pageModel.meta().defaultLang() != null && allowed.contains(pageModel.meta().defaultLang()))
        return pageModel.meta().defaultLang();
    return allowed.get(0);
}
```

Les overrides i18n par tenant sont gérés via une table `i18n_override` liée au tenant et à la locale.

---

## 8. Sécurité, multi-tenant & templates

- `PageTemplate` géré par super admin.
- `PageModel` géré par super admin (tous tenants) ou tenant admin (son tenant).
- RLS s’applique sur `tenant_id`.
- Adapter HTTP doit vérifier l’accès via `accesscontrol`.

---

## 9. Domaines existants (référence)

Exemples de domaines : `accesscontrol`, `audit`, `draw`, `ticket`, `session`, `tenantconfig`, `pagemodel`, `identity`, `reporting/*`.

Le domaine pagemodel reste orchestrateur sans dupliquer de logique métier critique.
