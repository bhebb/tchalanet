# Rapport d'audit — Catalogues & RLS

Ce document liste, pour les catalogues du serveur, l'état actuel vis‑à‑vis de la règle RLS (Row-Level Security) normative, les occurrences où du code passe explicitement un tenantId dans des requêtes (violation de la règle), et des recommandations actionnables et priorisées.

Contexte rapide

- RLS est utilisé comme mécanisme d'isolation par tenant via les variables de session PostgreSQL (`app.current_tenant`, `app.deleted_visibility`).
- Règle fondamentale (OpenSpec / `docs/conventions/persistence/rls.md`) : le code applicatif ne doit jamais ajouter de filtres tenant en SQL ; le filtrage est assuré par la base.
- Les clefs de cache restent tenant‑aware (la clé de cache doit inclure le tenant) — cela reste correct.

But de ce rapport

- Fournir une vue claire, par catalogue, des endroits à corriger (read impls / repositories), et des recommandations concrètes.
- Indiquer ce que j'ai déjà modifié (module `catalog/settings`) et les prochaines étapes proposées.

Checklist (ce que contient ce rapport)

- [x] Inventaire des catalogues couverts ici
- [x] Liste des fichiers concernés et extraits pertinents (read impl & repositories)
- [x] Statut RLS pour chaque catalogue (OK / À corriger)
- [x] Recommandations techniques précises et priorisées
- [x] Plan d'actions proposé

---

Scope

- J'ai inclus les catalogues suivants : `drawchannel`, `i18n`, `pagemodeltemplate`, `pricing`, `settings`, `theme`, `plan`.
- Par demande explicite, je n'ai pas approfondi `game`, `resultslot`, `tenant` (ils ne sont pas tenantés ou ne nécessitent pas de correction ici).

Rappel : ce qui est interdit

- Dans `catalog/*/internal/read/*` : il est interdit d'appeler des repository methods qui prennent explicitement `tenantId` et construisent un predicate tenant dans la clause WHERE. La variable `app.current_tenant` doit suffire.

---

1. catalog/settings — STATUT : corrigé (CHANGEMENT APPLIQUÉ)

- Fichiers pertinents :

  - `src/main/java/com/tchalanet/server/catalog/settings/internal/read/SettingsCatalogImpl.java` (implémentation read)
  - `src/main/java/com/tchalanet/server/catalog/settings/internal/persistence/SettingRepository.java` (repository)
  - `src/main/java/com/tchalanet/server/catalog/settings/internal/persistence/SettingEntity.java` (entité — étend `BaseTenantEntity`)
  - Cache : `src/main/java/com/tchalanet/server/catalog/settings/internal/cache/SettingsCacheNames.java`

- Problème détecté (avant correction) :

  - Les méthodes du read-side utilisaient des signatures `findBy...AndTenantId(...)` et passaient `tenantId` depuis `SettingsCatalogImpl`.
  - Ceci contourne la règle RLS.

- Correction appliquée :

  - Ajout de méthodes repository non‑tenantées destinées au read-side :
    - `findByActiveTrueAndDeletedAtIsNullAndLevel(...)` (déjà existante)
    - nouvelles méthodes :
      - `findByActiveTrueAndDeletedAtIsNullAndLevelAndOutletId(...)`
      - `findByActiveTrueAndDeletedAtIsNullAndLevelAndOutletIdAndNamespaceIn(...)`
      - `findByActiveTrueAndDeletedAtIsNullAndLevelAndTerminalId(...)`
      - `findByActiveTrueAndDeletedAtIsNullAndLevelAndTerminalIdAndNamespaceIn(...)`
  - `SettingsCatalogImpl` a été modifié pour appeler ces méthodes sans passer `tenantId` (conserver `tenantId` uniquement pour la clé de cache).

- Emplacement du fichier modifié (nouveau état):

  - `src/main/java/com/tchalanet/server/catalog/settings/internal/persistence/SettingRepository.java` (méthodes sans tenantId ajoutées)
  - `src/main/java/com/tchalanet/server/catalog/settings/internal/read/SettingsCatalogImpl.java` (utilise maintenant les méthodes sans tenantId)

- Recommandations supplémentaires :
  - Ajouter un test d'intégration RLS minimal : 2 tenants, set_config('app.current_tenant', ...), puis valider que la lecture sans tenantId renvoie uniquement les données du tenant courant.

---

2. catalog/i18n — STATUT : À CORRIGER (PRIORITÉ : HAUTE)

- Fichiers pertinents :

  - `src/main/java/com/tchalanet/server/catalog/i18n/internal/read/I18nOverridesCatalogImpl.java`
  - `src/main/java/com/tchalanet/server/catalog/i18n/internal/persistence/I18nOverrideRepository.java`
  - `src/main/java/com/tchalanet/server/catalog/i18n/internal/persistence/I18nOverrideEntity.java`
  - Cache : `src/main/java/com/tchalanet/server/catalog/i18n/internal/cache/I18nOverridesCacheNames.java`

- Occurrences (exemples trouvés) :

  - Dans `I18nOverridesCatalogImpl` :
    - `repository.findFirstByTenantIdAndLocaleAndI18nKeyAndActiveTrueAndDeletedAtIsNull(tenantId.value(), locale, i18nKey)`
    - `repository.findByTenantIdAndLocaleAndActiveTrueAndDeletedAtIsNull(tenantId.value(), locale)`
    - `repository.findByTenantIdAndActiveTrueAndDeletedAtIsNull(tenantId.value())`

  Ces lignes montrent que le read-side passe explicitement `tenantId` à la requête.

- Pourquoi c'est un problème :

  - Cela duplique le scoping tenant côté applicatif et contourne le mécanisme RLS (incohérence potentielle si `app.current_tenant` diffère ou pour les jobs/batchs).

- Recommandation technique concrète :

  1. Ajouter des méthodes repository non‑tenantées destinées au read-side (exemples) :
     - `List<I18nOverrideEntity> findByLocaleAndActiveTrueAndDeletedAtIsNull(String locale)`
     - `Optional<I18nOverrideEntity> findFirstByLocaleAndI18nKeyAndActiveTrueAndDeletedAtIsNull(String locale, String i18nKey)`
     - `List<I18nOverrideEntity> findByActiveTrueAndDeletedAtIsNull()` si besoin pour listing complet
  2. Modifier `I18nOverridesCatalogImpl` pour appeler ces méthodes sans passer `tenantId`. Garder `tenantId` uniquement dans la clé de cache (`@Cacheable(... key = "#tenantId.value()+':' + #locale + ':' + #i18nKey")`).
  3. Conserver les méthodes `findByTenantId...` dans le repo pour usages Admin/Write si nécessaire (contrôler que seules les couches admin y accèdent).
  4. Ajouter test d'intégration RLS (voir settings) pour valider la correction.

- Plan d'implémentation proposé (ordre) :
  - Ajouter méthodes repo sans tenantId.
  - Mettre à jour `I18nOverridesCatalogImpl`.
  - Lancer get_errors / build minimal et tests d'intégration RLS.

---

3. catalog/pagemodeltemplate — STATUT : À CORRIGER (PRIORITÉ : HAUTE)

- Fichiers pertinents :

  - `src/main/java/com/tchalanet/server/catalog/pagemodeltemplate/internal/read/PageModelTemplateCatalogImpl.java`
  - `src/main/java/com/tchalanet/server/catalog/pagemodeltemplate/internal/persistence/PageModelTemplateRepository.java`
  - Cache : `src/main/java/com/tchalanet/server/catalog/pagemodeltemplate/internal/cache/PageModelTemplateCacheNames.java`

- Occurrence :

  - `PageModelTemplateCatalogImpl` appelle `repository.findAllByTenantIdAndDeletedAtIsNull(tenantId.value())` (passage direct de tenantId en param).

- Recommandation :
  - Ajouter / utiliser une méthode repository sans tenantId (par ex. `findAllByDeletedAtIsNull()` ou `findAllByIsSystemTrueAndDeletedAtIsNull()` selon le cas) pour la lecture catalog.
  - Modifier `PageModelTemplateCatalogImpl` pour utiliser la méthode non‑tenantée et laisser RLS scoper.
  - Garder `tenantId` dans la clé cache mais ne pas le passer en SQL.

---

4. catalog/pricing — STATUT : À CORRIGER (PRIORITÉ : HAUTE)

- Fichiers pertinents :

  - `src/main/java/com/tchalanet/server/catalog/pricing/internal/read/PricingCatalogImpl.java`
  - `src/main/java/com/tchalanet/server/catalog/pricing/internal/persistence/PricingOddsJpaRepository.java`
  - Cache : `src/main/java/com/tchalanet/server/catalog/pricing/internal/cache/PricingCacheNames.java`

- Occurrence :

  - `PricingCatalogImpl.oddsFor` utilise `repo.findFirstByTenantIdAndGameCodeAndBetTypeAndBetOptionAndActiveIsTrueAndDeletedAtIsNull(tenant, gameCode, betType, betOption)` — passage de `tenant`.

- Recommandation :
  - Ajouter une méthode repo sans tenant param :
    - `Optional<PricingOddsEntity> findFirstByGameCodeAndBetTypeAndBetOptionAndActiveIsTrueAndDeletedAtIsNull(String gameCode, BetType betType, Short betOption)`
  - Modifier `PricingCatalogImpl` pour appeler la version sans tenant; conserver `tenantId` dans la clé cache.
  - Ajouter test d'intégration RLS pour confirmer comportement.

---

5. catalog/drawchannel — STATUT : OK / Conforme (PRIORITÉ : Basse)

- Fichiers pertinents :

  - `src/main/java/com/tchalanet/server/catalog/drawchannel/internal/read/DrawChannelCatalogImpl.java`
  - `DrawChannelRepository`, `DrawChannelGameRepository`
  - Cache : `catalog:drawchannel:...` (déjà normalisé)

- Observations :

  - `DrawChannelCatalogImpl` contient explicitement des notes RLS et n'ajoute pas de filtre tenant (il appelle `findByActiveTrueOrderBySortOrderAsc()` ou `findAllByOrderBySortOrderAsc()` pour liste globale). C'est conforme.
  - Le repository contient des méthodes `findByTenantId...` utiles pour admin flows ; laisser ces méthodes pour admin seulement.

- Recommandation :
  - Conserver l'approche actuelle ; lors de nouveaux ajouts, respecter la règle RLS (ne pas appeler les méthodes `findByTenantId...` depuis read impls).

---

6. catalog/theme — STATUT : À vérifier / potentiellement à corriger (PRIORITÉ : MOYENNE)

- Observations :

  - Quelques services relatifs aux thèmes (hors `catalog/` strict) utilisent `findByTenantId(...)`. Si `theme` est tenant-scoped et doit être couvert par RLS, préférer la même correction : read-side sans tenant param et repos tenant-agnostiques.

- Recommandation :
  - Si vous voulez que tout soit uniforme, appliquer la même règle : implémenter des repo methods non‑tenantées et laisser RLS scoper.
  - Sinon documenter explicitement l'exception (si theme est géré différemment).

---

Résumé des fichiers à modifier / actions recommandées (priorité & proposition de patch)

Priorité haute (corriger rapidement) :

- `catalog/i18n`:
  - Ajouter méthodes repo non‑tenantées (voir section i18n) et modifier `I18nOverridesCatalogImpl`.
- `catalog/pagemodeltemplate`:
  - Remplacer l'appel `findAllByTenantIdAndDeletedAtIsNull(...)` par une variante sans tenantId ou combinaison `findByTenantIdIsNull()` + `findByDeletedAtIsNull()` selon besoin (mais ne PAS passer tenantId depuis la couche read).
- `catalog/pricing`:
  - Ajouter méthode repo non‑tenantée et modifier `PricingCatalogImpl.oddsFor`.

Priorité moyenne :

- `catalog/theme` — vérifier et harmoniser si désiré.

Priorité basse / maintenance :

- Conserver les méthodes `findByTenantId...` dans les repositories pour les flows admin/écriture mais s'assurer qu'elles ne sont appelées que depuis `internal/write` ou services admin (et non depuis `internal/read`).
- Ajouter ArchUnit règles pour empêcher les erreurs futures (catalog.read → repo methods tenantId). Voir `75-catalog-rules.md` pour recommandations d'enforcement.

Plan d'actions proposé (concret)

1. Validez ce rapport.
2. J'implémente automatiquement, par ordre :
   a) `catalog/i18n` (repo + read impl)
   b) `catalog/pagemodeltemplate` (repo/read)
   c) `catalog/pricing` (repo/read)
   - pour chaque étape : editer le repo, editer le read impl, run `get_errors` et signaler tout conflit.
3. Ajouter un petit test d'intégration RLS pour un catalogue corrigé (ex: settings) — je peux en fournir la PR séparée.
4. Ajouter une règle ArchUnit (optionnel, mais recommandé) pour bloquer regressions.

Ce que j'ai déjà fait

- Normalisé les noms de cache dans plusieurs catalogues (`catalog:...`).
- Corrigé `catalog/settings` pour ne plus passer tenantId depuis le read-side (modifications dans `SettingRepository` & `SettingsCatalogImpl`).

Fichiers modifiés (résumé)

- `src/main/java/com/tchalanet/server/catalog/settings/internal/persistence/SettingRepository.java` — ajouts de méthodes sans tenantId pour outlet/terminal reads.
- `src/main/java/com/tchalanet/server/catalog/settings/internal/read/SettingsCatalogImpl.java` — now uses the tenant‑agnostic repo methods.

Prochaine action (attendue de vous)

- Validez ce rapport (révisions/observations). Une fois validé je peux appliquer automatiquement les corrections sur `i18n`, `pagemodeltemplate` et `pricing` (commits distincts par module), puis exécuter les vérifications (`get_errors`) et vous fournir les diffs pour revue.

Annexes (extraits pertinents)

- Exemple (extrait de `I18nOverridesCatalogImpl`) — lignes problématiques :

```java
// Exemple (à remplacer par variante sans tenantId)
repository.findFirstByTenantIdAndLocaleAndI18nKeyAndActiveTrueAndDeletedAtIsNull(tenantId.value(), locale, i18nKey)
```

- Exemple (extrait de `PageModelTemplateRepository`) :

```java
List<PageModelTemplateEntity> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId);
```

- Exemple (extrait de `PricingCatalogImpl`) :

```java
Optional<PricingOddsEntity> opt = repo.findFirstByTenantIdAndGameCodeAndBetTypeAndBetOptionAndActiveIsTrueAndDeletedAtIsNull(
    tenant, gameCode, betType, betOption);
```

---

Si vous validez : je lance les corrections automatiquement pour `i18n`, `pagemodeltemplate` et `pricing` (une PR / patch par module), puis je vous fournis les fichiers modifiés et les résultats de `get_errors`. Voulez‑vous que je commence maintenant ?

Fin du rapport. (Si vous voulez, je peux aussi créer une check‑list GitHub/PR template pour la revue.)
