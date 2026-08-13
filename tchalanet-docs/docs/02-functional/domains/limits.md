# Limites — Domaine fonctionnel

Les limites protègent le tenant contre le risque commercial : exposition excessive sur une sélection, mise trop élevée par ticket ou par ligne, sélections bloquées sur un tirage. Elles s'évaluent au moment de la vente et s'appliquent en cascade selon la portée.

---

## 1. Concept : règles et portées

### 1.1 RuleKey — les règles disponibles

| RuleKey | Stateless/Stateful | Ce qu'elle contrôle | Param principal |
|---|---|---|---|
| `MAX_STAKE_PER_LINE` | stateless | Mise max sur une seule ligne de ticket | `valueCents` |
| `MAX_LINES_PER_TICKET` | stateless | Nombre max de lignes sur un ticket | `maxCount` |
| `MAX_STAKE_PER_TICKET` | stateless | Mise totale max pour tout le ticket | `valueCents` |
| `MAX_STAKE_PER_BET_TYPE_PER_TICKET` | stateless | Mise max par type de pari dans un ticket | `valueCents` |
| `MAX_STAKE_PER_SELECTION_PER_TICKET` | stateless | Mise max sur une même sélection dans un ticket | `valueCents` |
| `BLOCK_SELECTION_PER_DRAW` | stateless | Bloque une ou plusieurs sélections pour un tirage | `selectionKeys` |
| `BLOCK_BET_TYPE` | stateless | Bloque complètement un type de pari | `betTypes` |
| `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` | **stateful** | Mise totale déjà exposée sur une sélection pour un tirage donné | `valueCents` |
| `MAX_SALES_COUNT_PER_SELECTION_PER_DRAW` | **stateful** | Nombre de ventes déjà enregistrées sur une sélection pour un tirage | `maxCount` |

Les règles **stateless** n'utilisent que le ticket en cours. Les règles **stateful** lisent `draw_exposure` (projection des ventes confirmées).

### 1.2 Portées (scopes)

Les règles sont configurées par portée. La hiérarchie de résolution, du moins spécifique au plus spécifique :

```
TENANT(10) < DRAW_CHANNEL(30) < SELLER_TERMINAL(50) < AGENT(60)
```

| Scope | Score de spécificité | Ce qu'il cible |
|---|---|---|
| `TENANT` | 10 | Tout le tenant |
| `DRAW_CHANNEL` | 30 | Un canal de tirage spécifique |
| `SELLER_TERMINAL` | 50 | Un terminal de vente spécifique |
| `AGENT` | 60 | Un utilisateur agent spécifique |

**Règle de résolution :** pour chaque RuleKey, la règle active du scope le plus spécifique l'emporte. Si aucune règle n'est configurée pour le scope le plus fin, on remonte vers les scopes plus larges.

### 1.3 Configuration (LimitAssignment)

Un `LimitAssignment` = une règle configurée pour un scope précis.

Exemple JSON — limite de mise par ticket de 3 000 HTG pour le canal NY_MIDI :

```json
{
  "ruleKey": "MAX_STAKE_PER_TICKET",
  "scopeType": "DRAW_CHANNEL",
  "scopeId": "draw-channel-uuid-ny-midi",
  "enabled": true,
  "onBreach": "BLOCK",
  "params": {
    "valueCents": 300000
  }
}
```

Cela signifie : pour toute vente sur le canal NY_MIDI, un ticket ne peut pas dépasser 3 000 HTG. Si la limite est dépassée, la vente est bloquée.

---

## 2. Comment configurer les limites

### 2.1 Via l'interface admin web

**Page Configuration du tenant** (`/app/admin/setup/config`) → section **Limites**

- Affiche les règles du tenant groupées par catégorie (VENTE, PAYOUT…)
- Portée `TENANT` par défaut
- Cliquer "Modifier" ouvre le dialog `UpsertLimitDialogComponent` :
  - Choisir la portée : `TENANT`, `DRAW_CHANNEL` ou `SELLER_TERMINAL`
  - Saisir la valeur + le comportement sur breach (`BLOCK` / `WARN`)

**Page Draw detail** (`/app/admin/draws/:drawId`) → bouton **"Bloke nimero"**

- Visible uniquement si `draw.status === 'OPEN'`
- Dialog `BlockNumberQuickDialogComponent` :
  - Scope par défaut : `DRAW_CHANNEL`
  - Radio "Tout le tenant" pour basculer sur `TENANT`
  - Picker `ActiveDrawChannelSelectComponent` : liste les canaux actifs, filtre local, pré-sélectionne le canal du draw si `channelId` est injecté

### 2.2 Via l'API directement

Créer ou modifier un LimitAssignment :

```http
PUT /admin/policies/limits/assignments
```

Corps attendu : `ruleKey`, `scopeType`, `scopeId` (si DRAW_CHANNEL ou SELLER_TERMINAL), `enabled`, `onBreach`, `params`.

Lister les règles disponibles :

```http
GET /admin/policies/limits/rules
```

Retourne le catalogue JSON des RuleKey avec metadata UI (label, description, catégorie, paramsTemplate).

---

## 3. Cascade et override

### 3.1 Résolution runtime

Au moment d'une vente, `LimitResolver` reçoit tous les assignments actifs pour les scopes applicables au contexte. Il sélectionne, pour chaque RuleKey, la règle dont le score de spécificité est le plus élevé.

```
TENANT(10) → DRAW_CHANNEL(30) → SELLER_TERMINAL(50) → AGENT(60)
             ↑ le score le plus élevé gagne
```

**Exemple pratique :**

| Scope | RuleKey | Valeur |
|---|---|---|
| TENANT | MAX_STAKE_PER_TICKET | 10 000 HTG |
| DRAW_CHANNEL (NY_MIDI) | MAX_STAKE_PER_TICKET | 5 000 HTG |
| SELLER_TERMINAL (ST1) | MAX_STAKE_PER_TICKET | 2 000 HTG |

Pour ST1 vendant sur NY_MIDI : limite effective = **2 000 HTG** (SELLER_TERMINAL, score 50, gagne).

### 3.2 Résolution selon le contexte BFF

| BFF | Endpoint | Scopes résolus | Raison |
|---|---|---|---|
| Admin draw overview | `GET /admin/draws/{id}/overview` | TENANT + DRAW_CHANNEL | L'admin n'est pas contextualisé à un terminal spécifique |
| POS draw detail | `GET /tenant/cashier/draws/{id}/detail` | TENANT + DRAW_CHANNEL + SELLER_TERMINAL | Auth SELLER_TERMINAL, sellerTerminalId dans le JWT Firebase |

### 3.3 Comportement sur breach

| `onBreach` | Effet |
|---|---|
| `BLOCK` | Opération rejetée immédiatement |
| `WARN` | Opération acceptée, notice retournée au caller |
| `REQUIRE_APPROVAL` | Autonomy consulté (V2+) |

---

## 4. Exposition draw (draw_exposure)

### 4.1 Qu'est-ce que draw_exposure

`draw_exposure` est une **projection dérivée** — elle agrège les mises confirmées par draw, scope, betType et selectionKey.

Elle représente : le risque potentiel créé par les ventes confirmées.

Elle ne représente **pas** : les payouts, les annulations, les remboursements.

### 4.2 Alimentation

Après chaque vente confirmée, `TicketPlacedEvent` → `ExposureProjectorAdapter` écrit sur tous les scopes applicables :

```
TENANT + DRAW_CHANNEL + SELLER_TERMINAL (si sellerTerminalId présent dans le contexte)
```

L'écriture est idempotente (`processedEvent.markProcessedIfAbsent` par handler_key + eventId).

### 4.3 Règles stateful qui l'utilisent

- `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` : stakeTotal sur la sélection ≤ limite configurée
- `MAX_SALES_COUNT_PER_SELECTION_PER_DRAW` : salesCount sur la sélection ≤ limite configurée

### 4.4 Consistance éventuelle

Entre la vente et l'application de l'exposition, une vente concurrente peut lire un état légèrement ancien. Ce comportement est acceptable en V1.

---

## 5. Voir l'exposition — côté admin web

### 5.1 Draw detail — section "Numéros à risque"

Visible si `effectiveLimits` contient `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` **et** `exposureAlerts` n'est pas vide.

Chips colorées par ratio (stakeTotal / limitCents) :

| Ratio | Couleur |
|---|---|
| < 0.5 | Vert |
| 0.5 – 0.79 | Orange |
| ≥ 0.8 | Rouge |

### 5.2 API directe

Vue agrégée draw + limites effectives + alertes :

```http
GET /admin/draws/{drawId}/overview
```

Retourne `effectiveLimits` (résolution TENANT + DRAW_CHANNEL) + `exposureAlerts`.

Vue brute paginée des lignes draw_exposure :

```http
GET /admin/policies/limits/exposure
```

---

## 6. Voir l'exposition — côté POS mobile

### 6.1 Section "Numéros chauds" (draw report)

Sur la page rapport d'un draw OPEN (`SellerTerminalDrawReportPage`, route `/pos/reports/draw-report`), le mobile affiche les top sélections + chips d'exposition.

Source : `GET /tenant/cashier/draws/{drawId}/detail`

- La résolution inclut SELLER_TERMINAL (scope le plus spécifique disponible au POS)
- **topSelections** : top 5 par mise totale
- **exposureAlerts** : sélections avec ratio ≥ 0.5

Chips : vert / orange / rouge (mêmes seuils que le web).

Le ratio est affiché en clair : "90% du plafond" (clé i18n `pos.reports.exposure_ratio`).

---

## 7. Source of truth

| Composant | Localisation |
|---|---|
| Moteur de règles + modèle domaine | `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/limitpolicy/DOMAIN_LIMITPOLICY.md` |
| BFF admin (overview draws + limits management) | `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/tenantadmin/FEATURE_TENANTADMIN.md` |
| BFF POS (draw detail) | `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/pos/FEATURE_POS.md` |
| UI admin Angular | `apps/admin-portal/src/app/features/limits/` |
| UI mobile Flutter | `tchalanet-mobile/lib/features/cashier/home/` |
