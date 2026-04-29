# Feature Ops (BFF)

> BFF d'exploitation plateforme pour déclencher des jobs techniques, consulter leurs exécutions et orchestrer des actions ops multi-domaines.

---

## 1. Rôle & objectifs

- Exposer des endpoints `/platform/ops/**` protégés pour les super admins.
- Orchestrer des commandes core existantes sans porter d'invariants métier.
- Gérer les batch gates et la visibilité opérationnelle des jobs.

---

## 2. Frontières d'architecture

`features.ops` est autorisé parce qu'il agrège des capacités transverses :

- `common.batch` pour registre, lancement, gates et statut d'exécution.
- `catalog.settings` pour stocker les gates opérationnels.
- `core.drawresult` et `core.draw` pour les commandes de fetch/apply de résultats.

Le module ne doit pas contenir de `CommandHandler` ni de `VoidCommandHandler`.
Les mutations unitaires restent dans les domaines core propriétaires.

---

## 3. Écart documenté

`OpsBatchService` écrit les gates via `catalog.settings.internal.persistence.SettingRepository`.
Cet accès est un écart temporaire et borné : il existe uniquement pour piloter des paramètres techniques de batch.

Limites obligatoires :

- aucune règle métier tenant ou draw dans `features.ops`;
- aucune écriture directe dans les tables core;
- aucune commande métier implémentée dans `features.ops`;
- migration cible : exposer un contrat de write côté `catalog.settings.api` pour remplacer l'accès repository interne.

---

## 4. Pattern refresh results

`POST /platform/ops/draw-results/refresh` est une orchestration BFF :

1. vérifie le gate `RESULTS_EXTERNAL_REFRESH`;
2. envoie `FetchExternalResultsWindowCommand` à `core.drawresult`;
3. envoie `ApplyExternalResultsWindowCommand` à `core.draw`;
4. retourne une réponse consolidée.

Il n'existe pas de command handler `RefreshExternalResultsWindowCommand` dans `features.ops`.
