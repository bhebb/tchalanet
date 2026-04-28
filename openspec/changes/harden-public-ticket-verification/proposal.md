## Why

L'audit `2026-04-26-sales-pipeline-audit.md` (§Pipeline B, §Top 5 #4) a identifié plusieurs failles dans `VerifyPublicTicketQueryHandler` qui exposent publiquement (sans authentification) des informations sensibles ou incohérentes :

1. **Fuite d'identifiants internes** : `maskAddress` n'efface que `line1/line2/region/postalCode/normalizedKey` mais conserve `address.id` et `address.tenantId` (UUIDs internes du tenant). Le `ticketId` et `drawId` sont aussi exposés en clair.
2. **`payoutStatus` incohérent** : calculé sur `potentialPayout` (montant pré-tirage) au lieu de `winningAmount` (gain réel post-tirage). Un ticket résolu `LOST` affiche `POTENTIAL_WIN` à un client public, et un ticket `WON` réellement gagnant n'a pas de marqueur distinct.
3. **Catch silencieux** : `try { ... } catch (Exception ignored) {}` autour du chargement outlet/address — masque toute panne (DB, RLS, port indisponible) sans log ni metric.
4. **Fenêtre de visibilité non auditée** : `SettingsCatalog.resolve(...)` lève → fallback silencieux à 14 jours sans log ; aucun mécanisme pour vérifier que le tenant a bien configuré sa policy.
5. **Mask terminal faible** : 8 premiers chars d'un UUID + `…` — suffisant pour rapprochement avec données internes.
6. **Champ `outletName` exposé** sans contrôle (raison sociale outlet) — choix probablement intentionnel mais non documenté.

Sans ces corrections, la surface publique de vérification de ticket fuit des identifiants tenant et donne un statut payout potentiellement mensonger.

## What Changes

- **[Mask address]** `VerifyPublicTicketQueryHandler.maskAddress` :
  - Remplacer le record `Address` retourné par un nouveau record `MaskedAddress(city, country)` exposé dans `TicketVerificationResult`.
  - Plus aucun champ identifiant (`id`, `tenantId`, `addressId`) côté DTO public.
- **[Mask ticketId / drawId]** `TicketVerificationResult` :
  - Supprimer `ticketId: TicketId` du DTO public — non nécessaire (le client identifie le ticket par `publicCode`).
  - Supprimer `drawId: DrawId` du DTO public — exposer à la place `drawDate: LocalDate` + `drawChannelCode: String` (informations métier sans UUID interne).
- **[`payoutStatus` réel]** `VerifyPublicTicketQueryHandler.toVisibleResult` :
  - Calculer `payoutStatus` sur `ticket.resultStatus` + `ticket.settlementStatus` + `ticket.winningAmount`.
  - Nouvelles valeurs : `PENDING_DRAW` (NOT_RESULTED), `WON_UNCLAIMED` (WON + UNSETTLED), `WON_PAID` (WON + SETTLED), `LOST`, `VOID`, `EXPIRED`.
  - Conserver `potentialTotalPayout` informatif mais ne plus le coupler au statut.
- **[Catch propre]** Remplacer le `try { ... } catch (Exception ignored) {}` par :
  - Catch ciblé sur `DataAccessException` / `IllegalStateException`.
  - Log WARN structuré : `log.warn("Outlet enrichment failed for verify publicCode={} : {}", code, e.getMessage())`.
  - Metric Prometheus `tch_sales_verify_outlet_enrichment_failure_total` (label : tenant).
- **[Visibility config]** `SettingsCatalog` lookup :
  - Catch ciblé `RestClientException` / `IllegalStateException`.
  - Log WARN explicite si fallback `14` est utilisé.
  - Metric `tch_sales_verify_visibility_fallback_total`.
- **[Mask terminal]** Renforcer `maskTerminal` : retourner uniquement `terminalLabel` (alias court depuis `TerminalReaderPort.findById(...).label()`) — supprimer l'exposition partielle de l'UUID.
- **[Tests]** Couverture exhaustive masquage + payoutStatus + fallback config.

## Capabilities

### New Capabilities

- `public-ticket-verification`: Définit le contrat de la vérification publique d'un ticket : (a) shape exacte du DTO `TicketVerificationResult` exposé publiquement (champs autorisés, masqués, et interdits), (b) règle de calcul du `payoutStatus` à partir de l'état réel du ticket (`saleStatus`, `resultStatus`, `settlementStatus`, `winningAmount`), (c) source de la fenêtre de visibilité (`SettingsCatalog`) avec fallback documenté + alerté, (d) traitement explicite des erreurs d'enrichissement (catch ciblé + log + metric), (e) absence d'identifiants internes UUID (`ticketId`, `drawId`, `address.id`, `tenantId`) dans la réponse publique.

## Impact

- **Code modifié** :
  - `core.sales.application.query.handler.VerifyPublicTicketQueryHandler.java` — refonte `toVisibleResult`, `maskAddress`, `maskTerminal`, `resolveVisibilityDays`, `payoutStatus`
  - `core.sales.domain.model.TicketVerificationResult.java` — refonte du record (suppression `ticketId`, `drawId`, `outletAddress: Address` ; ajout `drawDate`, `drawChannelCode`, `outletAddress: MaskedAddress`)
  - `core.sales.application.query.handler.VerifyPublicTicketQueryHandler` — accès au `Draw` via `DrawLookupPort` pour obtenir `drawDate` et `drawChannel.code()`
- **Code créé** :
  - `core.sales.domain.model.MaskedAddress` (record `city, country`)
  - Tests unitaires `VerifyPublicTicketQueryHandlerTest` (14 scenarios : visible/expired × résolu/non/void × payout réel)
- **API** :
  - **BREAKING (public API)** : `TicketVerificationResult` change de shape (suppression `ticketId`, `drawId`, `outletAddress` détaillé ; ajout `drawDate`, `drawChannelCode`, `outletAddress: MaskedAddress`).
  - Valeurs de `payoutStatus` étendues : ajout `PENDING_DRAW`, `WON_UNCLAIMED`, `WON_PAID`, `VOID` ; `POTENTIAL_WIN` et `NO_PAYOUT` deviennent dépréciées (signifient désormais `PENDING_DRAW`).
  - Coordination avec front public (`/v/{publicCode}`) requise.
- **Observabilité** : 2 nouvelles metrics Prometheus, 2 nouveaux logs WARN structurés.
- **Docs** :
  - `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md` §3 / §9
  - `tchalanet-docs/docs/02-functional/flows/verify-ticket.md` — refonte du tableau "surfaces sensibles exposées" + nouvelles valeurs `payoutStatus`
- **Non scope** :
  - Rate-limiting `/public/tickets/**` (couvert par `secure-sales-ticket-endpoints`)
  - Standardisation `ApiResponse<T>` sur public verify (couvert par `secure-sales-ticket-endpoints`)
  - Cryptographic signing du `publicCode` (déjà non-faisable sans renouvellement DB ; suivi futur)
