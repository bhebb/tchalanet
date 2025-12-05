# NEWS

Domaine : News (actualités publiques + gestion admin)

---

## 🎯 Objectifs du domaine

Le domaine **News** gère l’agrégation et la publication d’actualités sur Tchalanet :

- News externes provenant du fournisseur (LotteryDaily RSS)
- News internes créées et modifiées par l’administrateur
- Possibilité de cacher une news (interne ou externe) sans la supprimer
- Mise en cache pour performance et rafraîchissement contrôlé
- Affichage sur la page publique et sur la page admin

Ce domaine n’est pas DDD, mais une _vertical slice_ simple composée de :

- models (`record`)
- services
- controllers
- ports
- cache layer

---

## 🧩 Structure du slice

```text
features/news
├── shared/
│    ├── LotteryNewsModels.java
│    ├── NewsStatus.java
│    ├── port/
│    │    ├── NewsProviderPort.java
│    │    └── NewsCachePort.java
│    └── service/
│         ├── InternalNewsService.java
│         ├── ExternalNewsService.java
│         ├── HiddenNewsService.java
│         └── NewsAggregationService.java
├── publicview/
│    ├── PublicNewsService.java
│    └── PublicNewsController.java
├── admin/
│    ├── AdminNewsService.java
│    └── AdminNewsController.java
```

---

## ✨ Modèle principal

`LotteryNewsModels.LotteryNewsFeedSnapshot`

Snapshot complet d’un flux de news :

```java
record LotteryNewsFeedSnapshot(
    Instant fetchedAt,
    List<LotteryNewsArticle> articles
) {}
```

### Sources

- **interne** → géré via `InternalNewsService`
- **externe** → résultant du feed RSS (via ROME + `NewsProviderPort`)

---

## 🔤 Statuts éditoriaux

```java
public enum NewsStatus {
  DRAFT,      // visible seulement en admin
  PUBLISHED,  // visible publiquement (si non "hidden")
  ARCHIVED    // plus visible nulle part
}
```

`ARCHIVED` = équivalent interne du « hidden permanent ».

---

## 🗂 Services

### 1. `ExternalNewsService`

- Fetch du feed LotteryDaily via `NewsProviderPort`
- Parse du XML RSS
- Mise en cache (`newsExternalKey`) pour 24h

### 2. `InternalNewsService`

- Stockage des news internes dans un snapshot dédié (`newsInternalKey`)
- `save()`, `changeStatus()`, `findPublished()`
- Pas de base de données en V1

### 3. `HiddenNewsService`

- Stocke une liste d’IDs cachés dans un cache :
  - `hide(id)` → masque interne ou externe
  - `show(id)` → réaffiche
- TTL 24h → se réinitialise avec le cache

### 4. `NewsAggregationService`

Contrôle l’affichage côté public :

- internes `PUBLISHED` d’abord
- puis externes
- filtrage par `hiddenIds`
- tri par `publishedAt DESC`

---

## 🧭 API Admin (actions)

Exposée via `AdminNewsController` :

| Action           | Description                                            |
| ---------------- | ------------------------------------------------------ |
| `list()`         | Snapshot interne + externe + flag `hidden`             |
| `upsert()`       | Créer / éditer une news interne                        |
| `changeStatus()` | Modifier `DRAFT` / `PUBLISHED` / `ARCHIVED`            |
| `hide(id)`       | Cache une news (interne/externe)                       |
| `show(id)`       | Réaffiche                                              |
| `forceRefresh()` | Rafraîchit le feed externe et invalide le cache public |

---

## 🌐 API Public

### 1. Section news pour la home

```http
GET /api/public/news?limit=3
```

### 2. Page `/news` complète

```http
GET /api/public/news/all
```

---

## 🔐 Rôles

- **public** → lecture uniquement
- **admin** → toutes actions internes/externe (sauf modifier le contenu externe)

---

## 📝 Notes futures (V2+)

- Détails article → `GET /api/public/news/{id}`
- Rendu Markdown / rich HTML contrôlé
- Pagination sur la page publique
