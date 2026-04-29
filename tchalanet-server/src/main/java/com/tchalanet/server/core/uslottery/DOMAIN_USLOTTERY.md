# Domaine US Lottery

## 1. Rôle du domaine

Le domaine **uslottery** est responsable de l’intégration des résultats de loteries
américaines (NY, Florida, …) dans le système Tchalanet.

Il agit comme un **fournisseur externe spécialisé**, chargé de :

- récupérer les résultats depuis des APIs publiques/partenaires,
- normaliser ces résultats,
- fournir un résultat exploitable au domaine `draw`.

👉 Le domaine **ne gère pas** :

- la persistance des résultats,
- le cycle de vie des tirages,
- le settlement,
- la publication publique.

Ces responsabilités appartiennent exclusivement au domaine **`draw`**.

---

## 2. Responsabilités

### Ce que fait `uslottery`

- Interroger des providers externes (NY, FL).
- Normaliser les données brutes (ordre, validation, typage).
- Fournir un `ExternalDrawResult` compatible avec le domaine `draw`.
- Éviter les appels excessifs aux APIs externes (throttling / backoff).
- Supporter un déclenchement **batch** et **ops**.

### Ce que `uslottery` ne fait pas

- Créer ou modifier des `Draw`.
- Appliquer des règles métier de jeu.
- Décider si un tirage est valide.
- Stocker des résultats en base.

---

## 3. Modèle métier

### 3.1 `LatestDraw` (Value Object central)

```java
public record LatestDraw(
    UsLotteryProvider provider,
    String externalGameKey,
    String externalDrawType,
    String channelCode,
    LocalDate drawDate,
    OffsetDateTime occurredAtUtc,
    Instant fetchedAtUtc,
    DrawMain numbers,
    DrawExtras extras,
    ResultQuality quality,
    String origin,
    Map<String, String> meta
)
Rôle

Snapshot immuable d’un résultat externe.

Sert de transport interne entre provider → adapter draw.

Invariants

numbers est validé et ordonné.

channelCode est obligatoire.

drawDate + channelCode identifient un tirage.

Clé d’idempotence

provider + ":" + channelCode + ":" + drawDate

3.2 DrawMain
public record DrawMain(List<String> ordered)


Valeur ajoutée

Ordre strict conservé.

Validation des chiffres.

Taille contrôlable (requireSize).

Séparation claire des numéros principaux.

3.3 DrawExtras
public record DrawExtras(
    List<Integer> extraNumbers,
    Map<String, String> attributes
)


Utilité

Support future-proof (fireball, bonus, multiplicateur).

Typage explicite.

Peut être vide (DrawExtras.empty()).

3.4 ResultQuality
COMPLETE | PARTIAL | SUSPECT


Permet au domaine draw de :

accepter,

refuser,

ou mettre en attente un résultat externe.

4. Ports
4.1 Entrants

Aucun.
Le domaine est appelé uniquement par draw.

4.2 Sortants
LatestDrawProviderClient
List<LatestDraw> fetchLatestDraws();


Implémenté par NY / FL.

Retourne des résultats normalisés.

UsLotterySyncStatePort
boolean shouldFetch(String key, Instant now, boolean force);
void markSuccess(String key, Instant now);
void markEmpty(String key, Instant now);
void markError(String key, Instant now);
void clear(String key);


Rôle

Gérer le throttling et le retry.

Éviter de bombarder les APIs externes.

5. Gestion du throttling (cache)

Le cache ne stocke pas des résultats, il stocke un état de tentative.

États possibles

SUCCESS → ne plus fetch

EMPTY → retry plus tard (ex: +30 min)

ERROR → retry plus tard (backoff)

FORCE → bypass (ops)

Cas concret

Tirage à 12:00

Appel batch à 12:05 → vide → markEmpty

Retry à 12:35

Ops peut forcer immédiatement via clear(key) ou force=true

6. Intégration avec draw
Point d’intégration unique
ExternalDrawResultPort


Implémentation :

UsLotteryExternalDrawResultPortAdapter


Matching

DrawSource.US_LOTTERY

channelCode (MIDDAY vs EVENING)

scheduledAt.toLocalDate() == drawDate

7. Batch & Ops

Les jobs batch vivent dans draw

uslottery est appelé :

par FetchAndApplyExternalResultCommandHandler

via ExternalDrawResultPort

Un endpoint ops peut déclencher un refresh forcé

8. Architecture & règles

Domaine sans persistance

Pas de @Entity

Immutabilité stricte

Dépendance unidirectionnelle :

draw → uslottery

9. Configuration active

La source de vérité provider est `application-uslottery.yaml`.
`application.yaml` ne doit pas dupliquer `tch.us-lottery.providers.*`.

Providers MVP :

- NY : Numbers / Win4
- FL : Pick 3 / Pick 4
- GA : Cash 3 / Cash 4 horaires
- TX : Pick 3 / Daily 4
- TN : Cash 3 / Cash 4, provider désactivé par défaut dans le seed MVP

GameCodes hors scope MVP : `US_NY_TAKE5_EVE`, `US_FL_LOTTO`.

10. État actuel

✔ Domaine stable
✔ Modèle clair
✔ Cache maîtrisé
✔ Intégration propre avec draw




```
