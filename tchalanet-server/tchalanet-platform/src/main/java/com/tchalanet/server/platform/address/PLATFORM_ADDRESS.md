# Platform Capability `platform.address` — Address Validation & Formatting

> Archetype : Application Service Module.

## 1. Rôle

Valider, normaliser et formater des adresses physiques selon les règles locales (Haïti en priorité).

**Ce module fait** :
- Valider la structure d'une adresse (`AddressApi.validate(AddressInput)`).
- Normaliser/formater une adresse en format d'affichage.
- Référentiel des régions/communes/sections communales (si applicable).

**Ce module ne fait pas** :
- Géolocalisation / geocoding externe.
- Stockage d'adresses métier (les domaines core stockent leurs propres adresses).

## 2. Structure

```text
platform/address/
  api/
    AddressApi.java           ← validate(AddressInput) → ValidationResult, format(AddressInput) → String
    model/
      AddressInput.java
      AddressView.java
      ValidationResult.java
  internal/
    service/
    persistence/              ← ReferentielRegionRepository (si applicable)
    config/
```

## 3. Règles

- Peut être stateless si le référentiel est en mémoire/cache.
- Pas de RLS si les données sont purement référentielles (non-tenant-scoped).
- Consommé par les modules qui ont besoin de valider des adresses (outlets, users…).
