
# Platform Capability `platform.address` — Address Validation & Formatting

> Archetype : Application Service Module.


## Rôle

Valider, normaliser, stocker et exposer les adresses physiques selon les règles locales (Haïti en priorité).

**Ce module fait** :
- Upsert d’une adresse principale par tenant (`upsertTenantPrimary`)
- Upsert générique multi-adresse (pour outlet/user)
- Lecture d’adresse (`get`)
- Validation stricte des champs (line1, city, country obligatoires)

**Ce module ne fait pas** :
- Géolocalisation / geocoding externe
- Stockage d’adresses métier hors tenant (core stocke ses propres adresses)


## Surface API

- `AddressApi` (Java) :
  - `upsertTenantPrimary(TenantId, AddressInput)` : mono-adresse par tenant
  - `get(TenantId, AddressId)` : lecture d’une adresse
- Modèles : `AddressInput`, `AddressView`
- Service interne : `AddressCrudService` (expose aussi un upsert multi-adresse)

Pas d’endpoint REST public direct (API Java consommée par d’autres modules platform/core).


## Règles et limitations

- Unicité d’adresse principale par tenant (contrainte DB)
- Upsert multi-adresse pour outlet/user (clé de déduplication)
- Pas de RLS (unicité par tenant, pas de scope utilisateur)
- Pas de géocodage ni de validation postale externe
- Validation stricte des champs à la création

## Intégration

- Consommé par les modules qui gèrent des entités avec adresse (outlet, user, tenant)
- Peut être étendu pour référentiel régions/communes si besoin
