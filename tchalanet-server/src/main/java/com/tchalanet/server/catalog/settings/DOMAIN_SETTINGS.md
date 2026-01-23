# Domaine Settings

> Ce fichier est un **template** pour documenter le domaine backend.
> Copie/complète les sections ci-dessous (voir `docs/DOMAIN_TEMPLATE.md`).
> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/catalog.md` (settings referenced)

---

## 1. Rôle du domaine

**Responsabilité principale**

> Décris en une phrase la responsabilité clé du domaine.
> **Ce que le domaine fait**

- Maintenir un registre des clés (`AppSettingRegistry`).
- Stocker les valeurs (`AppSettingValue`) par tenant/outlet.
- Exposer une lecture consolidée (tenant + outlet overrides).
  **Ce que le domaine ne fait pas**
- Ne porte pas des règles métier (les domaines interprètent les settings).

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `AppSettingRegistry`: key (snake_case), type (string/int/bool/json), scope (tenant|outlet), defaultValue.
- `AppSettingValue`: key, scopeId (tenantId|outletId), value.

### Invariants métier

- Clé unique par registry.
- Validation du type.
  > Valeur métier clé :
  > ...

---

## 3. Cas d’utilisation (ports d’entrée)

- `RegisterSettingKeyCommandHandler`
- `SetSettingValueCommandHandler`
- `GetEffectiveSettingsQueryHandler(tenantId, outletId?)`

---

## 4. Ports de sortie (dépendances externes)

- `AppSettingRegistryRepoPort`
- `AppSettingValueRepoPort`

---

## 5. Mapping & DTOs (convention)

- MapStruct pour mapper infra.web.model ↔ application.command/query.model
- Records immuables pour DTO simples, Lombok si nécessaire
- DTO `SettingKeyRequest` / `SettingKeyResponse`, `SettingValueRequest` / `SettingValueResponse`.

---

## 6. Règles métier importantes

- ...

---

## 7. Intégration avec les autres domaines

Dépend de : ...
Utilisé par : ...

---

## 8. Notes techniques

- Multi-tenant; RLS pour values tenant/outlet.
- Wrappers d’ID; UUID en JPA.
- Cache recommandé pour lecture effective.

---

## 9. Incohérences / TODO

- Définir conventions de nommage des clés.
- Spécifier fallback quand outlet n’a pas de valeur (utiliser tenant).
