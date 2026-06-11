# Platform Capability `platform.tenantconfig` — Tenant Configuration

## Rôle

Gérer le cycle de vie des tenants et leur configuration opérationnelle.  
Source de vérité pour l'identité tenant, les paramètres internes, le calendrier métier, la locale et le fuseau horaire.

**Ce module fait** :
- Création, activation, suspension des tenants (`TenantConfigApi`)
- Lecture et mise à jour de la configuration tenant (identité, paramètres internes)
- Résolution du fuseau horaire (`TenantZoneApi`), locale (`TenantLocaleApi`), calendrier métier (`TenantBusinessCalendarApi`)
- Accès aux configurations communication et document du tenant

**Ce module ne fait pas** :
- Évaluation des limites métier (→ `core.limitpolicy`)
- Gestion des jeux par tenant (→ `platform.tenantgame`)
- Profil utilisateur (→ `platform.identity`)
- Authentification (→ `common.security`)

---

## Enums — depuis `catalog.tenant`

### `TenantStatus`

| Valeur | Sens |
|---|---|
| `DRAFT` | Tenant créé, provisioning non terminé |
| `ACTIVE` | Tenant opérationnel |
| `SUSPENDED` | Suspendu (paiement, compliance…) |
| `REJECTED` | Demande de tenant rejetée |
| `ARCHIVED` | Tenant archivé (fin de vie) |

### `TenantType`

| Valeur | Sens |
|---|---|
| `BORLETTE` | Opérateur borlette/loterie |
| `RESEAU` | Réseau de revendeurs |
| `AMBULANT` | Vendeur ambulant |

---

## API — `TenantConfigApi`

```java
void     createTenant(CreateTenantRequest)
void     activateTenant(ActivateTenantRequest)
void     suspendTenant(SuspendTenantRequest)
void     updateTenantIdentity(UpdateTenantIdentityRequest)
void     updateTenantInternalSettings(UpdateTenantInternalSettingsRequest)

TenantConfigView getTenantById(GetTenantByIdRequest)
TenantConfigView getTenantByCode(GetTenantByCodeRequest)
List<TenantConfigView> listTenants(ListTenantsRequest)

TenantInternalCommunicationConfig getTenantCommunicationConfig(GetTenantByIdRequest)
TenantInternalDocumentConfig      getTenantDocumentConfig(GetTenantByIdRequest)
```

## API — interfaces secondaires

```java
// TenantZoneApi
ZoneId resolveTenantZone(TenantId)

// TenantLocaleApi
Locale         resolveDefaultLocale(TenantId)
String         resolveDefaultLanguage(TenantId)
List<String>   resolveSupportedLanguages(TenantId)

// TenantBusinessCalendarApi
TenantBusinessDayView resolveBusinessDay(TenantId, OutletId, LocalDate)
```

---

## Modèle — `TenantConfigView`

| Champ | Type | Sens |
|---|---|---|
| `tenantId` | `TenantId` | — |
| `code` | `String` | Code stable du tenant |
| `name` | `String` | Nom affiché |
| `type` | `TenantType` | Type d'opérateur |
| `timezone` | `ZoneId` | Fuseau horaire opérationnel |
| `currency` | `Currency` | Devise (ex: HTG) |
| `status` | `TenantStatus` | Statut lifecycle |
| `activeThemeId` | `ThemePresetId` | ID du preset actif |
| `activeThemeCode` | `String` | Code du preset actif (affichage) |
| `address` | `AddressView` | Adresse principale |
| `internalSettings` | `JsonNode` | Config interne (communication, document, rules, locale) |

### `TenantInternalSettings` — structure du blob JSON

```
internalSettings:
  communication:
    buyerTicketDelivery:
      sms:       { enabled, amount, currency, paidBy }
      whatsapp:  { enabled, amount, currency, paidBy }
      email:     { enabled, amount, currency, paidBy }
  document:
    receipt:
      { enabled, displayName, headerMessage, footerMessage,
        defaultPaperSize, showQrCode, showSellerName,
        showOutletName, showPotentialPayout, defaultTemplateKey }
  rules:     { ... }
  locale:    { ... }
```

### `TenantBusinessDayView`

| Champ | Type | Sens |
|---|---|---|
| `tenantId` | `TenantId` | — |
| `businessDate` | `LocalDate` | Jour évalué |
| `open` | `boolean` | Jour ouvrable ? |
| `reasonCode` | `String` | Code raison si fermé |
| `label` | `String` | Libellé |

---

## Intégration

- RLS actif (toutes les tables tenant-scoped)
- Caching des lookups fréquents (timezone, locale, status)
- Consommé par `TchContextFilter` pour bootstrapper le contexte de requête
- `createTenant` déclenche le provisioning dans `features.platformadmin`

---

## Promotion par défaut à l'onboarding — SPÉCIFIÉ, non implémenté

> Source de vérité : `tchalanet-server/openspec/changes/maryaj-gratis-auto-selection-v1/`.

À l'onboarding d'un nouveau tenant, une campagne `Maryaj gratuit` est
instanciée depuis le template plateforme `DEFAULT_MARYAJ_GRATIS`
(template -> instance tenant, jamais de campagne globale partagée en runtime).

- V1 : seed du template + **commande admin interne** d'instanciation pour un
  tenant donné. Le hook automatique dans le provisioning est un follow-up
  (le provisioning actuel crée seulement tenant + admin).
- Tenants existants : **pas de backfill automatique silencieux** — tâche ops
  explicite avec dry-run.
- La campagne instanciée appartient au tenant : désactivable et modifiable
  (montant, éligibilité) via l'admin promotion (`core.promotion`).

Détails du template : `core/promotion/promotion_design.md` §16.

---

## Références

- Provisioning tenant : `tchalanet-docs/docs/02-functional/flows/tenant-onboarding.md`
- Contexte HTTP : `tchalanet-server/docs/conventions/context/request-context.md`
