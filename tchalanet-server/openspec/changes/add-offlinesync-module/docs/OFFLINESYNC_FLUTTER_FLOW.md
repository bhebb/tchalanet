# OFFLINESYNC — Flutter Android flow

## 1. Stockage local

Utiliser Drift/SQLite avec transaction locale atomique.

Tables locales :

```text
offline_grant_cache
offline_code_cache
offline_sale
offline_sale_line
offline_sync_outbox
```

## 2. Vente offline atomique

Dans une seule transaction locale :

1. vérifier grant local et signature serveur ;
2. vérifier `now <= validUntil` ;
3. réserver le prochain code local `AVAILABLE` ;
4. créer sale locale ;
5. signer le payload avec clé privée Ed25519 device ;
6. marquer le code local `CONSUMED_LOCAL` ;
7. imprimer le ticket avec mention OFFLINE.

## 3. Statuts locaux

```text
DRAFT
SIGNED_PENDING_SYNC
SYNCING
SYNC_ACCEPTED
SYNC_REJECTED_TECH
PROMOTED
BUSINESS_REJECTED
NEEDS_ADMIN_REVIEW
```

## 4. UI minimale

- Badge `Mode offline` visible.
- Compteur ventes non synchronisées.
- Alerte avant expiration `validUntil`.
- Alerte avant expiration `syncAcceptedUntil`.
- Bouton `Synchroniser maintenant`.
- Réimpression marquée `DUPLICATA`.
- Mode offline forcé visible et auditable côté backend au prochain sync.

## 5. Déclencheurs de sync

- retour réseau ;
- ouverture app ;
- fermeture session ;
- manuel vendeur ;
- tâche périodique contrôlée.

## 6. Horloge device

Le device peut afficher des alertes basées sur son horloge, mais le serveur reste source de vérité.

Le payload signé inclut `clientSoldAt`; le serveur compare avec les fenêtres du grant.
