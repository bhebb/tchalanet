## Context

`VerifyPublicTicketQueryHandler` (`src/main/java/com/tchalanet/server/core/sales/application/query/handler/VerifyPublicTicketQueryHandler.java`) sert l'endpoint public `GET /public/tickets/verify/{publicCode}`. C'est la seule surface du domaine sales accessible sans authentification.

### Faille 1 — Fuite d'identifiants

`TicketVerificationResult` expose actuellement :

- `ticketId: TicketId` (UUID interne)
- `drawId: DrawId` (UUID interne)
- `outletAddress: Address` (record complet, dont `id` et `tenantId` non masqués)

Le `maskAddress` actuel :

```java
return new Address(a.id(), a.tenantId(), null, null, a.city(), null, a.country(), null, null, false, null, null);
```

remplace les champs sensibles par `null` mais conserve `id` et `tenantId` (premières positions du record).

### Faille 2 — `payoutStatus` incohérent

Logique actuelle (ligne 100) :

```java
String payoutStatus = potentialTotal.signum() > 0 ? "POTENTIAL_WIN" : "NO_PAYOUT";
```

`potentialTotal` est calculé sur `lines[].potentialPayout` — c'est le payout théorique avant tirage, indépendant du résultat. Un ticket `LOST` (résultat connu, perdu) affiche `POTENTIAL_WIN` ; un ticket `WON` mais déjà payé n'a pas de signal distinctif.

### Faille 3 — Catch silencieux

Lignes 106-126 :

```java
try {
  var terminalOpt = terminalReader.findById(...);
  // ... lookup outlet, address ...
} catch (Exception ignored) {}
```

Toute exception (DB down, RLS interdit, port absent) est silencieusement absorbée. Le client reçoit une réponse partielle (`outletName=null, outletAddress=null`) sans aucun signal côté ops.

### Faille 4 — Fallback visibility

Lignes 165-179 : `SettingsCatalog.resolve(...)` peut lever ; tout type d'exception → return `14` (default). Aucun log, aucune metric. Si la config tenant est cassée en silence, tous les tickets passent en mode "visible 14 jours" sans alerte.

### Faille 5 — Mask terminal

Lignes 188-192 :

```java
private String maskTerminal(TerminalId terminalId) {
  if (terminalId == null) return null;
  var s = terminalId.toString();
  return s.length() <= 8 ? s : s.substring(0, 8) + "…";
}
```

8 premiers caractères d'un UUID v4 = entropie suffisante pour identifier de manière quasi-unique le terminal côté ops via reverse-lookup. `Terminal` a un champ `label` métier (numéro POS humain) — c'est la valeur correcte à exposer.

## Goals / Non-Goals

**Goals:**

- Aucun UUID interne ne doit transiter par le DTO public `TicketVerificationResult`
- `payoutStatus` doit refléter l'état réel du ticket (sale + result + settlement + winning)
- Toute défaillance d'enrichissement doit être loggée et metricée
- Tout fallback de configuration (visibility days) doit être loggé et metricé
- Le mask terminal doit utiliser le label métier, pas un UUID tronqué

**Non-Goals:**

- Cryptographic signing du `publicCode` (le code est déjà aléatoire 12 chars Base32 = ~60 bits ; brute-force atténué par rate-limit du change `secure-sales-ticket-endpoints`)
- Migration vers `ApiResponse<T>` sur l'endpoint (couvert par `secure-sales-ticket-endpoints`)
- Refonte du domaine `core.address` ou `core.outlet` pour fournir des "public projections"
- Audit log côté `core.audit` à chaque verify (volumétrie élevée, à voir séparément)

## Decisions

### D1 — Suppression vs masquage de `ticketId` et `drawId`

Trois options :

1. Supprimer du DTO
2. Hash one-way (HMAC) avec une seed serveur
3. Conserver mais documenter

**Décision** : option 1 (suppression). Le client public n'a aucun usage légitime de l'UUID interne du ticket — le `publicCode` est l'identifiant qu'il manipule. Pour le draw, on remplace par `drawDate: LocalDate` + `drawChannelCode: String` qui sont les valeurs métier exposables.

### D2 — Nouveau record `MaskedAddress`

Trois options :

1. Réutiliser `Address` avec masquage (statu quo, défectueux)
2. Nouveau record `MaskedAddress(city, country)` côté `core.sales.domain.model`
3. Réutiliser `Address` mais factory dédiée `Address.publicMask()`

**Décision** : option 2. Type-safe (impossible de retourner accidentellement un `Address` complet), explicite côté contrat public. Le record vit dans `core.sales.domain.model` car c'est une projection métier de sales pour son use case verify.

Justification additionnelle : aligne avec le pattern `TicketVerificationResult` lui-même (record dédié, pas un `Ticket` complet).

### D3 — Valeurs de `payoutStatus`

Nouvelles valeurs :

- `PENDING_DRAW` — ticket pas encore résolu (`NOT_RESULTED`)
- `WON_UNCLAIMED` — gagnant, payout non encore exécuté (`WON + UNSETTLED`)
- `WON_PAID` — gagnant, payout exécuté (`WON + SETTLED`)
- `LOST` — perdant
- `VOID` — annulé
- `EXPIRED` — au-delà de la fenêtre de visibilité

Anciennes valeurs (backward compat) :

- `POTENTIAL_WIN` — supprimée (était trompeuse)
- `NO_PAYOUT` — supprimée (était ambiguë)

**Décision BREAKING** assumée — le client public doit gérer les nouvelles valeurs. Les anciennes ne sont plus émises.

### D4 — Catch ciblé

Trois options :

1. Catch sur exception type spécifique (DataAccessException, NoSuchElementException)
2. Catch global avec log WARN (mieux que silent ignore)
3. Pas de catch, laisser remonter (mais 500 sur un cas mineur n'est pas désirable)

**Décision** : option 1 — catch sur les types attendus (`DataAccessException`, `IllegalStateException`). Log WARN + metric. Si une autre exception remonte (ex: `NullPointerException`), elle se propage → 500 légitime.

### D5 — Recharger le `Draw` ou enrichir le `Ticket`

Pour exposer `drawDate` et `drawChannelCode`, le handler doit charger le `Draw`. Trois options :

1. Ajouter `DrawLookupPort.findById(drawId)` dans le handler
2. Enrichir `Ticket` agrégat avec ces champs (cache)
3. Créer une projection JOIN dans `TicketReaderPort.findByPublicCodeWithDrawSummary(...)`

**Décision** : option 3 — éviter les N+1 queries et garder le handler simple. Nouveau port read-only `findByPublicCodeWithDrawSummary` dans `TicketReaderPort`, qui retourne un `TicketWithDrawSummary` (record). L'adapter JPA fait un JOIN ou récupère le `Draw` via `DrawLookupPort` et compose.

Note : option 1 plus rapide à implémenter ; on l'accepte comme MVP si l'option 3 nécessite trop de refactor — à trancher en review.

### D6 — `terminalLabel` au lieu de `terminalMasked`

Renommer le champ DTO pour explicitement indiquer qu'il contient le label métier, pas un masque crypto. `Terminal.label()` existe déjà côté `core.pos.domain.model.Terminal` (à vérifier — sinon ajouter).

## Risks / Trade-offs

- **[Risque] Cassure client public** : suppression `ticketId`/`drawId`/`outletAddress.id` + nouvelles valeurs `payoutStatus` = breaking API public. → Mitigation : coordination front + CHANGELOG explicite + version d'endpoint si nécessaire (`/public/v2/tickets/verify` en parallèle pendant transition).
- **[Risque] Perte d'info debug pour ops** : suppression `ticketId` rend le support harder (impossible de chercher un ticket par UUID depuis ce qu'a vu le client). → Mitigation : `publicCode` reste suffisant pour le support (lookup côté admin via `findByPublicCode`).
- **[Risque] Recharge Draw = latence** : option D5/3 ajoute un JOIN à chaque verify. → Mesuré : un verify est rare (par humain via QR), latence acceptable ; cache HTTP `no-store` côté client mais pas côté backend (à voir si Caffeine local justifié).
- **[Trade-off] Nouvelles valeurs `payoutStatus` plus expressives = plus complexe** : 6 valeurs au lieu de 3. → Acceptable, doc claire dans le flow MkDocs.
- **[Trade-off] `MaskedAddress(city, country)` perd la zone postale** : un client pourrait vouloir afficher "Port-au-Prince, HT 6110" pour validation. → Décision conservative : on accepte la perte ; preuve d'achat doit passer par le ticketCode en POS, pas par l'address.

## Migration Plan

1. Créer `MaskedAddress` record + tests
2. Refondre `TicketVerificationResult` (champs supprimés/ajoutés)
3. Adapter `VerifyPublicTicketQueryHandler.toVisibleResult` + `maskAddress` + `maskTerminal` + `payoutStatus`
4. Adapter `VerifyPublicTicketQueryHandler` pour charger le Draw (option D5)
5. Catch ciblé + logs + metrics
6. Tests unitaires `VerifyPublicTicketQueryHandlerTest` (14 scénarios)
7. Tests d'intégration `PublicTicketControllerIT` mis à jour
8. Documentation flow + DOMAIN_SALES
9. Coordonner avec front public — CHANGELOG `BREAKING (public API)`

Rollback : possible mais le DTO précédent est défectueux → préférer un fix-forward.

## Open Questions

- Q1 : Faut-il maintenir un endpoint v1 legacy (`/public/v1/tickets/verify/{publicCode}`) en parallèle de v2 le temps que le front migre ? → Pré-décision : non, pas d'historique de versionning d'API public, on assume la BREAKING en coordination.
- Q2 : Le `TerminalLabel` (alias humain) existe-t-il déjà dans `core.pos.domain.model.Terminal` ? À vérifier ; s'il n'existe pas, ce change l'ajoute (champ JPA + migration Flyway si nécessaire).
- Q3 : Faut-il un audit log par verify ? → Pré-décision : non v1 (volumétrie publique potentiellement élevée). À ré-évaluer si abuse détecté.
