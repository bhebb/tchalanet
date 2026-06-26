# Tchalanet Web — i18n split & translation architecture for Codex

## Status

Proposed — V0 i18n cleanup for admin/superadmin/seller-terminal navigation and pages.

## Context

The current `fr.json` has grown into a single large file mixing:

- generic actions (`view`, `refresh`, `save`, `cancel`),
- domain vocabulary (`seller-terminal`, `draw`, `odds`, `tenant`),
- sidenav labels,
- page-specific labels,
- reusable component labels,
- old concepts (`cashier`, `odds`, `bonuses`, `seller-terminal` visible to business users),
- duplicated labels across `common`, `nav`, `admin`, `platform`, `dashboard`, `cashier`, `public`, etc.

This causes inconsistent UX and makes it hard to reuse admin tenant pages inside the superadmin “support tenant” mode.

The goal is to move from one uncontrolled `fr.json` to a **multi-file i18n structure** with a strict logical taxonomy.

---

## Product language principles

### Surfaces

Tchalanet has three private surfaces:

| Surface | User profile | UX language |
|---|---|---|
| Superadmin / Platform | Technical/platform operator | More technical terms allowed: tenants, platform, ops, audit, cache, archives |
| Admin tenant | Business manager | Simple managerial terms: ventes, vendeurs, tirages, limites, commissions |
| Seller terminal | Sales operator on terminal/mobile | Minimal terms: vendre, tickets, vérifier, réimprimer, mes stats, PIN |

### Important language decisions

| Technical/current word | Preferred business label |
|---|---|
| Tenant | Espace / Mon entreprise in admin, Tenant in platform |
| Seller-terminal | Vendeur / Terminal vendeur |
| Cashier | Terminal vendeur / Vendeur |
| Draw | Tirage |
| Draw result | Résultat |
| Draw channel | Canal de tirage |
| Result slot | Slot de résultat / Créneau de résultat |
| Odds | Gains à payer / Barème de gains |
| Odds globales | Barème général |
| Odds par vendeur | Exceptions par vendeur |
| Commission default tenant | Commission générale |
| Commission seller-terminal | Commission par vendeur |
| Bonus / Prime | Do not expose in V0 — no current business concept |

---

## Target file structure

Use multiple physical files per language, but keep a single logical key tree after loading.

```text
apps/tch-portal/src/assets/i18n/
├── fr/
│   ├── common.json
│   ├── domain.json
│   ├── component.json
│   ├── surface-admin.json
│   ├── surface-platform.json
│   ├── surface-seller-terminal.json
│   ├── feature-admin-setup.json
│   ├── feature-admin-home.json
│   ├── feature-admin-draws.json
│   ├── feature-admin-sellers.json
│   ├── feature-admin-limits.json
│   ├── feature-admin-controls.json
│   ├── feature-admin-promotions.json
│   ├── feature-admin-reports.json
│   ├── feature-admin-tickets.json
│   ├── feature-admin-company.json
│   ├── feature-platform-tenants.json
│   ├── feature-platform-catalog.json
│   ├── feature-platform-ops.json
│   ├── feature-platform-support.json
│   ├── feature-platform-tchala.json
│   ├── feature-seller-terminal.json
│   ├── feature-auth.json
│   └── feature-public.json
├── en/
│   └── same file names
└── ht/
    └── same file names
```

The app should merge all files for the active locale into one translation object.

Angular code should not care which physical file contains a key.

Example usage:

```ts
this.translate.instant('common.action.refresh');
this.translate.instant('domain.sellerTerminal.plural');
this.translate.instant('surface.admin.nav.draws');
this.translate.instant('feature.adminDrawDetail.tabs.summary');
```

---

## Logical namespace rules

### 1. `common.*`

Only generic labels reused everywhere.

Do not put Tchalanet business terms here.

Example `fr/common.json`:

```json
{
  "common": {
    "action": {
      "view": "Voir",
      "details": "Détails",
      "refresh": "Actualiser",
      "save": "Enregistrer",
      "saving": "Enregistrement…",
      "cancel": "Annuler",
      "back": "Retour",
      "edit": "Modifier",
      "delete": "Supprimer",
      "search": "Rechercher",
      "filter": "Filtrer",
      "resetFilters": "Réinitialiser",
      "export": "Exporter",
      "print": "Imprimer",
      "verify": "Vérifier",
      "confirm": "Confirmer",
      "archive": "Archiver",
      "activate": "Activer",
      "deactivate": "Désactiver",
      "block": "Bloquer",
      "unblock": "Réactiver",
      "close": "Fermer",
      "open": "Ouvrir"
    },
    "state": {
      "loading": "Chargement…",
      "empty": "Aucun élément",
      "error": "Erreur",
      "notAvailable": "Non disponible",
      "notSet": "Non renseigné",
      "unknown": "Inconnu"
    },
    "validation": {
      "required": "Ce champ est obligatoire.",
      "email": "Adresse email invalide."
    },
    "yes": "Oui",
    "no": "Non"
  }
}
```

### 2. `domain.*`

One canonical place for Tchalanet business vocabulary.

Example `fr/domain.json`:

```json
{
  "domain": {
    "tenant": {
      "label": "Espace",
      "businessLabel": "Mon entreprise",
      "platformLabel": "Tenant"
    },
    "sellerTerminal": {
      "label": "Vendeur",
      "plural": "Vendeurs",
      "technicalLabel": "Terminal vendeur",
      "code": "Code vendeur",
      "pin": "PIN"
    },
    "draw": {
      "label": "Tirage",
      "plural": "Tirages",
      "status": {
        "scheduled": "Planifié",
        "upcoming": "À venir",
        "open": "Vente ouverte",
        "closed": "Vente fermée",
        "locked": "Vente bloquée",
        "cancelled": "Annulé",
        "resulted": "Résultat saisi",
        "archived": "Archivé"
      },
      "action": {
        "enterResult": "Entrer résultat",
        "lockSales": "Bloquer la vente",
        "unlockSales": "Réouvrir la vente",
        "cancelDraw": "Annuler le tirage"
      }
    },
    "ticket": {
      "label": "Ticket",
      "plural": "Tickets",
      "sell": "Vendre un ticket",
      "verify": "Vérifier un ticket",
      "reprint": "Réimprimer"
    },
    "limit": {
      "label": "Limite",
      "plural": "Limites",
      "system": "Limites système",
      "global": "Limite générale",
      "bySeller": "Par vendeur",
      "byNumber": "Par numéro",
      "byGame": "Par jeu",
      "byDraw": "Par tirage"
    },
    "commission": {
      "label": "Commission",
      "plural": "Commissions",
      "defaultRate": "Commission générale",
      "sellerRate": "Commission par vendeur"
    },
    "payoutOdds": {
      "label": "Gains à payer",
      "default": "Barème général",
      "sellerOverride": "Exceptions par vendeur"
    },
    "gamePricing": {
      "label": "Jeux & tarifs",
      "gamesSold": "Jeux vendus",
      "salePrices": "Tarifs de vente"
    }
  }
}
```

### 3. `component.*`

Labels for reusable components only.

Example `fr/component.json`:

```json
{
  "component": {
    "dataTable": {
      "empty": "Aucun résultat",
      "selectedCount": "{{count}} élément(s) sélectionné(s)",
      "actions": "Actions"
    },
    "domainActions": {
      "notAllowedStatus": "Action non disponible pour ce statut.",
      "requiresSelection": "Sélectionnez au moins un élément.",
      "requiresReason": "Un motif est requis."
    },
    "placeholder": {
      "title": "Écran en préparation",
      "message": "Cette fonctionnalité sera disponible prochainement."
    },
    "setupChecklist": {
      "progressTitle": "Progression de configuration",
      "advancedSettings": "Paramètres avancés"
    }
  }
}
```

### 4. `surface.*`

Shell and navigation labels by surface.

Do not put page content here.

Example `fr/surface-admin.json`:

```json
{
  "surface": {
    "admin": {
      "title": "Administration",
      "nav": {
        "home": "Accueil",
        "setup": "Configuration générale",
        "sellers": "Vendeurs",
        "sellersList": "Liste des vendeurs",
        "sellerNew": "Nouveau vendeur",
        "draws": "Tirages",
        "drawsAll": "Tous les tirages",
        "drawsOpen": "Tirages en cours",
        "drawsPast": "Tirages passés",
        "drawsMatrix": "Matrice des tirages",
        "drawsConfig": "Configuration des tirages",
        "limits": "Limites",
        "limitsSystem": "Limites système",
        "limitsGlobal": "Limite générale",
        "limitsSeller": "Par vendeur",
        "limitsNumber": "Par numéro",
        "limitsGame": "Par jeu",
        "limitsDraw": "Par tirage",
        "salesControls": "Contrôles de vente",
        "gamesPricing": "Jeux & tarifs",
        "payoutOdds": "Gains à payer",
        "commissions": "Commissions",
        "promotions": "Promotions",
        "maryajFree": "Maryaj gratis",
        "otherPromotions": "Autres promotions",
        "reports": "Rapports",
        "reportsSales": "Ventes",
        "reportsSellers": "Vendeurs",
        "reportsDraws": "Tirages",
        "reportsExports": "Exportations",
        "tickets": "Tickets",
        "ticketsList": "Liste des tickets",
        "ticketsSell": "Vendre",
        "ticketsVerify": "Vérifier",
        "company": "Mon entreprise",
        "companyIdentity": "Identité",
        "companyAddress": "Adresse",
        "companyAppearance": "Apparence",
        "companySettings": "Paramètres",
        "companySupport": "Support",
        "help": "Aide"
      }
    }
  }
}
```

Example `fr/surface-platform.json`:

```json
{
  "surface": {
    "platform": {
      "title": "Plateforme",
      "nav": {
        "overview": "Vue d’ensemble",
        "tenants": "Tenants",
        "tenantList": "Tous les tenants",
        "tenantOnboarding": "Onboarding tenant",
        "tenantAdmins": "Admins tenant",
        "supportTenant": "Support tenant",
        "references": "Référentiels",
        "games": "Jeux",
        "drawChannels": "Canaux de tirage",
        "drawChannelGames": "Jeux par canal",
        "resultSlots": "Slots de résultats",
        "resultSlotCalendars": "Calendriers des slots",
        "plans": "Plans",
        "pricing": "Pricing",
        "globalSettings": "Paramètres globaux",
        "themes": "Thèmes",
        "translations": "Traductions",
        "pageModelTemplates": "Templates de pages",
        "operations": "Opérations",
        "draws": "Tirages",
        "drawResults": "Résultats",
        "scheduledTasks": "Tâches planifiées",
        "cache": "Cache",
        "archives": "Archives",
        "audit": "Audit",
        "communicationTests": "Tests communication",
        "identitySync": "Synchronisation identité",
        "supportAndContent": "Support & contenu",
        "contactRequests": "Messages de contact",
        "news": "News publiques",
        "notifications": "Notifications",
        "contactConfig": "Configuration contact",
        "tchala": "Tchala",
        "tchalaSuggestions": "Suggestions",
        "tchalaImport": "Import",
        "tchalaCleanup": "Nettoyage",
        "accessSecurity": "Accès & sécurité",
        "permissions": "Permissions",
        "roles": "Rôles",
        "superAdmins": "Super admins",
        "users": "Utilisateurs",
        "backendKeys": "Clés publiques backend",
        "reports": "Rapports plateforme"
      },
      "supportTenant": {
        "banner": {
          "title": "Contexte tenant actif",
          "message": "Vous administrez {{tenantName}} en mode support plateforme.",
          "changeTenant": "Changer de tenant",
          "exit": "Quitter le contexte tenant"
        }
      }
    }
  }
}
```

Example `fr/surface-seller-terminal.json`:

```json
{
  "surface": {
    "sellerTerminal": {
      "title": "Terminal vendeur",
      "nav": {
        "home": "Accueil",
        "sell": "Vendre",
        "tickets": "Tickets",
        "verify": "Vérifier",
        "stats": "Mes stats",
        "profile": "Mon terminal"
      }
    }
  }
}
```

### 5. `feature.*`

Page-specific copy.

Example `fr/feature-admin-setup.json`:

```json
{
  "feature": {
    "adminSetup": {
      "title": "Configuration générale",
      "description": "Complétez les éléments nécessaires pour commencer à vendre.",
      "optional": "Optionnel",
      "operational": "Opérationnel",
      "progress": {
        "title": "Progression de configuration",
        "subtitle": "Suivi de l’état de préparation"
      },
      "section": {
        "identity": {
          "title": "Identité",
          "description": "Informations de base de votre entreprise.",
          "cta": "Voir"
        },
        "address": {
          "title": "Adresse",
          "description": "Ajoutez l’adresse de votre entreprise.",
          "cta": "Configurer l’adresse"
        },
        "games": {
          "title": "Jeux & tarifs",
          "description": "Choisissez les jeux vendus et les tarifs.",
          "cta": "Configurer les jeux"
        },
        "draws": {
          "title": "Tirages",
          "description": "Configurez les tirages disponibles pour la vente.",
          "cta": "Configurer les tirages"
        },
        "generatedDraws": {
          "title": "Tirages générés",
          "description": "Vérifiez les tirages prêts à être vendus.",
          "cta": "Voir les tirages"
        },
        "appearance": {
          "title": "Apparence",
          "description": "Personnalisez les couleurs et le thème.",
          "cta": "Configurer l’apparence"
        },
        "promotions": {
          "title": "Promotions",
          "description": "Activez les promotions disponibles.",
          "cta": "Configurer les promotions"
        },
        "sellers": {
          "title": "Vendeurs",
          "description": "Créez vos vendeurs après avoir complété les informations requises.",
          "locked": "Complétez d’abord l’adresse avant de créer vos vendeurs."
        }
      },
      "advancedSettings": {
        "title": "Paramètres avancés",
        "description": "Configuration réservée aux cas particuliers.",
        "action": "Configurer"
      }
    }
  }
}
```

Example `fr/feature-admin-draws.json`:

```json
{
  "feature": {
    "adminDraws": {
      "title": "Tirages",
      "description": "Suivez les ventes, les vendeurs et les résultats par tirage.",
      "filters": {
        "all": "Tous",
        "open": "En cours",
        "past": "Passés",
        "missingResult": "Résultats manquants"
      },
      "columns": {
        "draw": "Tirage",
        "date": "Date",
        "time": "Heure",
        "status": "Statut",
        "totalSold": "Total vendu",
        "ticketCount": "Tickets",
        "sellerCount": "Vendeurs",
        "result": "Résultat",
        "actions": "Actions"
      },
      "actions": {
        "viewDetail": "Voir détail",
        "enterResult": "Entrer résultat",
        "lockSales": "Bloquer la vente",
        "unlockSales": "Réouvrir la vente",
        "cancelDraw": "Annuler le tirage",
        "archive": "Archiver",
        "export": "Exporter"
      }
    },
    "adminDrawDetail": {
      "title": "Détail tirage",
      "tabs": {
        "summary": "Résumé",
        "sellers": "Vendeurs",
        "selections": "Sélections",
        "result": "Résultat",
        "tickets": "Tickets",
        "history": "Historique"
      },
      "summary": {
        "title": "Résumé",
        "totalSold": "Total vendu",
        "ticketCount": "Tickets vendus",
        "activeSellers": "Vendeurs actifs",
        "potentialPayout": "Gains potentiels",
        "resultStatus": "Résultat",
        "drawStatus": "Statut"
      },
      "sellerSales": {
        "title": "Ventes par vendeur",
        "seller": "Vendeur",
        "amountSold": "Montant vendu",
        "tickets": "Tickets",
        "estimatedCommission": "Commission estimée",
        "status": "Statut"
      },
      "topSelections": {
        "title": "Sélections les plus jouées",
        "selection": "Sélection",
        "amount": "Montant",
        "tickets": "Tickets",
        "exposure": "Exposition"
      }
    }
  }
}
```

---

## Superadmin reuse of admin pages

Do not duplicate admin feature translations under platform.

When superadmin opens a tenant-scoped admin page:

- reuse the same Angular component,
- reuse the same `feature.admin*` keys,
- reuse the same `domain.*` vocabulary,
- add only a platform support banner around it.

Example:

```html
<tch-platform-tenant-context-banner *ngIf="mode === 'platform-support'" />

<tch-admin-limits-page
  [mode]="mode"
  [tenantContext]="tenantContext"
/>
```

Translation usage inside the reused page:

```ts
this.translate.instant('feature.adminLimits.title');
this.translate.instant('domain.limit.bySeller');
this.translate.instant('common.action.save');
```

Translation usage for the platform support wrapper:

```ts
this.translate.instant('surface.platform.supportTenant.banner.title', { tenantName });
```

---

## Loader implementation options

### Option A — merge files at runtime

Create an HTTP loader that fetches all registered translation parts for a language and deep-merges them.

Example registry:

```ts
export const I18N_PARTS = [
  'common',
  'domain',
  'component',
  'surface-admin',
  'surface-platform',
  'surface-seller-terminal',
  'feature-admin-setup',
  'feature-admin-home',
  'feature-admin-draws',
  'feature-admin-sellers',
  'feature-admin-limits',
  'feature-admin-controls',
  'feature-admin-promotions',
  'feature-admin-reports',
  'feature-admin-tickets',
  'feature-admin-company',
  'feature-platform-tenants',
  'feature-platform-catalog',
  'feature-platform-ops',
  'feature-platform-support',
  'feature-platform-tchala',
  'feature-seller-terminal',
  'feature-auth',
  'feature-public'
] as const;
```

Pseudo-code:

```ts
@Injectable()
export class TchMultiFileTranslateLoader implements TranslateLoader {
  constructor(private http: HttpClient) {}

  getTranslation(lang: string): Observable<Record<string, unknown>> {
    return forkJoin(
      I18N_PARTS.map(part =>
        this.http.get<Record<string, unknown>>(`/assets/i18n/${lang}/${part}.json`)
      )
    ).pipe(map(parts => deepMergeAll(parts)));
  }
}
```

Use a deterministic deep merge. If two files define the same full key, fail in tests/lint.

### Option B — build-time merged file

Keep multiple source files, but generate one `fr.json`, `en.json`, `ht.json` during build.

This is safer for runtime performance and caching.

Suggested command:

```text
npm run i18n:build
```

Output:

```text
assets/i18n/fr.json
assets/i18n/en.json
assets/i18n/ht.json
```

Both options are acceptable. Prefer Option B if the current translate loader expects a single file.

---

## Migration plan

### Phase 1 — Add new structure without breaking existing pages

- Create the `fr/` folder with split files.
- Add `common`, `domain`, `component`, `surface`, and new `feature.admin*` keys.
- Keep the old `fr.json` or old keys temporarily.
- Point the new admin sidenav and setup page to new keys.
- Do not delete legacy keys yet.

### Phase 2 — Update admin V0 pages

Update keys used by:

- admin sidenav,
- admin setup page,
- admin home/dashboard,
- sellers,
- draws and draw detail,
- limits,
- controls,
- promotions,
- reports,
- tickets,
- company.

### Phase 3 — Update platform V0 pages

Update keys used by:

- platform sidenav,
- tenants,
- onboarding tenant,
- catalog/referentials,
- operations,
- support & content,
- Tchala,
- access & security,
- support tenant wrapper.

### Phase 4 — Seller terminal language cleanup

- Replace visible `cashier` labels with `sellerTerminal` keys.
- UI labels should say “Terminal vendeur”, “Vendeur”, “Vendre”, “Tickets”, “Vérifier”, “Réimprimer”, “Mes stats”, “Mon terminal”.
- Keep backend route names unchanged for now if needed.

### Phase 5 — Remove legacy/duplicated keys

Remove or deprecate:

- `nav.admin.odds`,
- `nav.admin.bonuses`,
- `nav.admin.more` if no longer used,
- `nav.cashier.*` once seller-terminal UI is migrated,
- visible `seller-terminal` labels in public/admin UX,
- `platform.nav.providers` if no V0 route exists,
- `platform.nav.createTenant` if onboarding handles provisioning,
- duplicated `refresh`, `view`, `save`, `cancel`, etc. under feature actions when `common.action.*` is enough.

---

## Rules for future keys

### Do

```ts
'common.action.refresh'
'domain.ticket.sell'
'domain.limit.bySeller'
'surface.admin.nav.draws'
'feature.adminDrawDetail.tabs.summary'
'surface.platform.supportTenant.banner.title'
```

### Do not

```ts
'admin.generatedDraws.actions.refresh'
'platform.tenants.action.viewDetails'
'cashier.quickActions.newTicket'
'admin.odds.title'
'admin.sellerTerminal.title'
```

Unless the label is truly page-specific and cannot be expressed through `common`, `domain`, or `component`.

---

## Acceptance criteria

- [ ] i18n files are split by language and domain according to this document.
- [ ] The app can load/merge translations without changing component usage semantics.
- [ ] Admin sidenav uses `surface.admin.nav.*` keys.
- [ ] Platform sidenav uses `surface.platform.nav.*` keys.
- [ ] Seller terminal sidenav uses `surface.sellerTerminal.nav.*` keys.
- [ ] Shared business terms come from `domain.*`.
- [ ] Shared actions come from `common.action.*`.
- [ ] Reusable component labels come from `component.*`.
- [ ] Admin feature pages use `feature.admin*.*` keys.
- [ ] Superadmin support-tenant pages reuse admin feature keys and only add a `surface.platform.supportTenant.banner.*` wrapper.
- [ ] No visible admin/seller-terminal label says `tenant`, `seller-terminal`, `cashier`, `odds`, or `bonus` unless explicitly intended for platform/technical pages.
- [ ] Legacy keys remain temporarily for compatibility, but are listed for cleanup.

---

## Notes for Codex

When editing code:

1. Do not rewrite every feature at once.
2. First add the new files and loader/merge support.
3. Migrate only the admin V0 sidenav and setup page first.
4. Keep old keys until no component references them.
5. Add a simple key-collision check if implementing build-time merge.
6. Prefer business labels in visible UI; technical names may remain in code identifiers.
7. Do not rename backend endpoints or route paths as part of this i18n change.
