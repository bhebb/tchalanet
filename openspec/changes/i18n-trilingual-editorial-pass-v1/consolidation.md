# Consolidation — doublons, mutualisation, clés mortes

Audit du 2026-08-05 sur les 4 050 clés web × 3 locales. Répond à trois questions :
qu'est-ce qui est dupliqué, qu'est-ce qui devrait vivre dans `common`/`domain`, qu'est-ce qui est mort.

---

## 1. Clés mortes — 31, après deux passes de vérification

| Namespace | clés | 1ʳᵉ détection | 2ᵉ détection | **vérifié** |
|---|---|---|---|---|
| `common` | 105 | 0 | 17 | **8** |
| `domain` | 118 | 30 | 17 | **17** |
| `component` | 120 | 49 | 10 | **6** |

> Il a fallu trois passes pour obtenir ce chiffre, et les deux premières étaient fausses dans des
> sens opposés. La première comptait 79 clés mortes qui étaient en fait résolues dynamiquement.
> La deuxième, en excluant les préfixes dynamiques, a raté que `common.weekday.*` (7 clés) et
> `common.receipt.promotion.*` sont construites à l'exécution — et a manqué 13 clés
> `domain.entity.*` réellement mortes que la première avait cachées derrière un préfixe partagé.

Familles **résolues dynamiquement** (chacune adossée à un fichier:ligne, pas supposée) :

| Préfixe | Preuve |
|---|---|
| `draw_channel.*` | clé envoyée par le serveur — `public-result-detail.model.ts:6` |
| `domain.bet.option.*` | `console-game-display.ts:266` |
| `domain.bet.type.*` | `console-game-display.ts:252` |
| `domain.result.status.*` | `public-result-detail.page.ts:69` |
| `common.game.*` | `pos-ticket-detail.page.ts:246` |
| `common.weekday.*` | `tenant-config-summary.component.ts:46` |
| `common.receipt.promotion.*` | `pos-ticket-detail.page.ts:258` — `` `common.${clean}` `` |
| `catalog.{game,bet_type,option}.*` | `game-label.pipe.ts:7,12,13` |
| `quickaction.*`, `layout.*` | `labelKey` des templates PageModel backend |
| `dashboard.fallback.*` | `assets/config/page-private-fallback.json` |
| `dashboard.period.*` | `period-selector.widget.ts:47` — **mais seules 4 valeurs de l'enum sont atteignables** |

Détail du piège originel :

- `draw_channel.*.label` (17) → **la clé est envoyée par le serveur**
  (`public-result-detail.model.ts:6` : *« Stable i18n key from the server »*).
- `domain.bet.option.*` (9) → construite en template literal :
  `` `domain.bet.option.${normalizeBetType(betType)}.${option}` `` (`console-game-display.ts:266`).
- `quickaction.*`, `layout.*` (44) → **`labelKey` des templates PageModel du backend**
  (`tchalanet-app/src/main/resources/pagemodel/templates/*.json`).
- `dashboard.fallback.*` (11) → `libs/shared-assets/public/assets/config/page-private-fallback.json`.

> Un `grep` de clé littérale sur le code web ne suffit pas à déclarer une clé i18n morte sur ce
> projet. Il faut couvrir : code web, templates PageModel backend, configs d'assets, et les
> constructions dynamiques.

### Supprimées (11 clés × 3 locales)

| Clé | Valeur FR | Pourquoi |
|---|---|---|
| `domain.draw.provider.{newYork,florida,georgia,texas}` | New York, Florida… | Le serveur envoie `providerLabel` en texte brut, pas une clé i18n |
| `domain.entity.sellerTerminal` | Terminal vendeur | Doublon de `domain.entity.seller`, avec le registre **interdit** |
| `domain.entity.sellerTerminals` | Terminaux vendeurs | idem de `domain.entity.sellers` |
| `dashboard.period.previous_day` | Jour précédent | Vestige d'un enum renommé — `DashboardPeriod` ne produit plus cette valeur |
| `dashboard.period.previous_week` | Semaine précédente | idem |
| `shell.error.backendUnavailable.title` | Serveur temporairement indisponible | Aucune référence. **Et toujours en français en EN et HT** |
| `shell.error.backendUnavailable.message` | La navigation et certaines fonctionnalités… | idem |
| `app.nav.dashboard` | Mon espace | Doublon orphelin de `nav.dashboard` (`surface-admin`), seul référencé |

### Conservées malgré leur inutilisation — décision requise

`domain.entity.{draws,promotion,promotions,result,results,seller,sellers,tenant,tenants,ticket,tickets}`
et `common.{print,verify,view,unblock,unknown,coming_soon,paginationAria,alphaNavAria}`
ne sont référencées nulle part **mais sont exactement les cibles de consolidation du Lot A**.

Les supprimer et vouloir consolider ensuite serait contradictoire. Deux options :

- **Les garder comme hub délibéré** — alors le Lot A implique de modifier le **code** pour pointer
  vers elles (`domain.entity.tickets` au lieu de `admin.reports.daily.column.tickets`). Ce n'est
  plus un travail de traduction, c'est un refactor de composants.
- **Les supprimer** — on renonce à la mutualisation et on se contente d'aligner les divergences
  (Lot B), ce qui règle 100 % du problème de qualité perçue pour 0 risque.

En attendant l'arbitrage, `domain.entity.tenant`/`tenants` ont été assainis
(« Tenant » → « Santral » / « Operator ») pour qu'aucune référence future n'hérite du registre interdit.

---

## 2. Mutualisation — 88 termes, 286 clés redondantes

88 termes existent **déjà** dans `common`/`domain` et sont malgré tout redéfinis ailleurs.
Mais **on ne peut pas les fusionner en bloc** : le même libellé FR ne recouvre pas toujours le même
concept. Quatre lots, dans cet ordre.

### Lot 0 — assainir le hub d'abord (bloquant)

On ne consolide pas vers une référence elle-même fautive. Ces entrées de `domain` portent le
registre interdit et doivent être corrigées **avant** que quoi que ce soit pointe vers elles :

| Clé | FR actuel | Problème |
|---|---|---|
| `domain.entity.tenant` | Tenant | terme de code ; → « Santral » (14 clés en dépendent) |
| `domain.entity.tenants` | Tenants | idem (8 clés) |
| `domain.entity.sellerTerminal` | Terminal vendeur | concept `Terminal` retiré ; → « Vendeur » |
| `domain.entity.sellerTerminals` | Terminaux vendeurs | idem ; → « Vendeurs » |
| `domain.entity.seller` | Vendeur | correct en FR, mais **HT diverge** : `Tèminal POS` / `Vandè` / `Tèminal` |

### Lot A — fusion sûre (~120 clés)

Même concept, même registre, traduction déjà cohérente dans les trois locales. Les clés locales
sont remplacées par un renvoi vers `common`/`domain`.

Exemples vérifiés cohérents : `common.cancel`, `common.close`, `common.edit`, `common.delete`,
`common.retry`, `common.block`, `common.archive`, `common.create`, `common.confirm`,
`common.filter`, `common.export`, `common.total`, `common.error.title`, `common.dateRange.from`,
`domain.entity.draw`, `domain.entity.game`, `domain.entity.ticket`, `domain.result.field.numbers`,
`domain.result.status.*`.

### Lot B — corriger la divergence, garder les clés séparées (~110 clés)

Le terme est le même, mais **la traduction diverge selon l'endroit**. C'est un défaut de
traduction, pas un défaut de structure : on aligne les valeurs, on ne fusionne pas les clés.

| FR | Divergence | Verdict |
|---|---|---|
| Tableau de bord | HT : `Tablo debò` / `Tablo bò` / `Tablo kontwòl` | 3 formes pour un mot — à unifier |
| Réinitialiser | HT : `Reyinisyalize` / `Reyajiste` / `Re-inisyalize` | idem |
| À configurer | EN : `Needs config` / `Needs setup` / `To configure` | idem |
| Vérifier un ticket | EN : `Verify a ticket` / `Check a ticket` / `Verify ticket` | idem — et c'est le CTA principal du site public |
| Canaux de tirage | HT : `Règ tiraj` / `Kanal tiraj` | `Kanal tiraj` est la forme du glossaire |
| Actions rapides | EN : `Quick actions` / `Quick Actions` | casse |
| Actif, Adresse, Imprimer, Type, Actualiser, Nom, Mariage, Désactivé, Tenant | EN ou HT = **le mot français** | non traduit — bug, pas divergence de style |

### Lot C — ne jamais fusionner (pièges vérifiés)

| Cas | Pourquoi c'est un piège |
|---|---|
| `catalog.option.Front` (« Avant » = position dans un pari) ↔ `platform.entityHistory.detail.before` (« Avant » = avant/après dans un diff) | **Homonymes parfaits en FR.** EN : `Front` vs `Before`. Fusionner casse l'un des deux |
| `common.field.name` (« Nom ») ↔ `admin.sellerTerminals.field.last_name` (« Nom », EN `Last name`) | **C'est le FR qui est faux** — devrait être « Nom de famille ». Fusionner propagerait l'erreur |
| `domain.result.field.date` (« Date ») ↔ `platform.entityHistory.column.changedAt` (« Date ») | `changedAt` devrait être « Modifié le ». Le FR est sous-spécifié |
| `domain.entity.tickets` (`Tikè yo`) ↔ en-têtes de colonnes (`Tikè`) | Le pluriel défini kreyòl est correct pour un label d'entité, **faux** pour un en-tête de colonne. Les deux formes sont justes |
| `common.weekday.*` ↔ `admin.settings.config.calendar.days.*` | Les secondes sortaient du bloc JSON dupliqué récupéré en phase 2 — à comparer avant de trancher |

> Le lot C est la raison pour laquelle cette consolidation ne doit pas être scriptée en masse.
> C'est exactement le geste — un remplacement global sans relecture — qui a produit les
> 283 fuites `seller-terminal` corrigées par ce change.

---

## 3. Divergences hors mutualisation

Au-delà des 88 termes ci-dessus : **188 termes FR reçoivent des traductions EN ou HT différentes
selon la clé**, sur 746 clés. Le lot B en couvre la partie qui recoupe `common`/`domain` ;
le reste est traité namespace par namespace dans la passe éditoriale (phase 3).

---

## Ordre d'exécution proposé

1. **Lot 0** — assainir `domain.entity.{tenant,tenants,seller,sellerTerminal,sellerTerminals}`.
2. **Supprimer les 5 clés mortes** de `component`.
3. **Lot B** — aligner les divergences (gain de qualité immédiat, aucun risque structurel).
4. **Lot A** — fusionner vers `common`/`domain`, un namespace à la fois, avec l'audit en garde-fou.
5. **Lot C** — laisser tel quel, documenté ici pour que personne ne « corrige » ces cas plus tard.
