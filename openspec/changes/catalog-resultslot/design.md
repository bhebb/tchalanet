# design.md

Change-id: catalog-resultslot

## Contexte architectural

Le `catalog` contient des données de référence déclaratives et doit rester sans logique métier. Les "result slots" sont des templates de créneaux horaires utilisés par le core pour planifier ou publier des résultats. Ils doivent donc être stockés dans `catalog` et exposés via une vue simple.

## Décisions clé

- Exposer uniquement des views DTO (`ResultSlotView`) qui contiennent les informations nécessaires au core pour créer des entités d'exécution.
- Catalog implémente uniquement des opérations CRUD simples (admin) et des requêtes read-only pour `core`.
- Caching: `listActive()` est @Cacheable (cache global) pour réduire la charge; update/delete doit évincer la cache.
- Le `core` est responsable du lifecycle (génération, statuts, annulation). Le catalogue ne doit pas émettre d'événements métier.

## Schéma de données (proposition minimal)

Table `result_slot` (catalog)

- id UUID PK
- slot_key VARCHAR UNIQUE
- provider VARCHAR
- timezone VARCHAR
- draw_time TIME (ou result_time)
- cutoff_seconds INT
- days_of_week VARCHAR (serialized pattern)
- default_source VARCHAR NULL
- active BOOLEAN DEFAULT TRUE
- created_at TIMESTAMP
- updated_at TIMESTAMP
- deleted_at TIMESTAMP NULL
- version INT (optimistic lock)

Note: cette table est globale (pas de tenant_id). Le catalogue est platform-scoped.

## Mapping JPA

- `ResultSlotJpaEntity` sans champ tenantId; inclure `deletedAt` pour soft-delete et `@Version` pour la colonne de version.
- `ResultSlotRepository` avec une query `findAllByActiveTrueAndDeletedAtIsNull()` ou une query plus fine si besoin.

## Contrats entre modules

- `ResultSlotCatalog.listActive()` renvoie les DTOs filtrés: `active = true` et `deleted_at IS NULL`.
- `core` doit appeler cette API pour obtenir les templates et créer des exécutions idempotentes côté core (unique key côté exécution si nécessaire).
- `resultslot` ne doit pas dépendre de `core` ou émettre d'événements métier.

## Évolution future

- Ajout d'une API paginée si le volume des rows devient conséquent.
- Gouvernance des migrations pour la migration de données existantes.
