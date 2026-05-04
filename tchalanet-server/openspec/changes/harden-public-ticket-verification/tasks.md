## Status: DRAFT

## 1. Modèle DTO `TicketVerificationResult`

- [ ] 1.1 Créer `core.sales.domain.model.MaskedAddress` (record `city: String, country: String`)
- [ ] 1.2 Refondre `TicketVerificationResult` :
  - Supprimer : `ticketId`, `drawId`, `outletAddress: Address`
  - Ajouter : `drawDate: LocalDate`, `drawChannelCode: String`, `outletAddress: MaskedAddress`
  - Renommer : `terminalMasked` → `terminalLabel`
- [ ] 1.3 Adapter le record nested `Line` si nécessaire (rester sur `gameCode, selection, stake, potentialPayout`)
- [ ] 1.4 Tests `TicketVerificationResultTest` — sérialisation JSON, vérifier qu'aucun champ identifiant interne n'apparaît

## 2. Enrichissement Draw

- [ ] 2.1 Décider en review : option A (port `DrawLookupPort.findById` dans handler) vs option B (nouveau `findByPublicCodeWithDrawSummary` dans `TicketReaderPort`)
- [ ] 2.2 Implémenter l'option choisie ; créer `TicketWithDrawSummary` record si option B
- [ ] 2.3 Charger `drawDate` et `drawChannel.code()` au moment du verify

## 3. `terminalLabel`

- [ ] 3.1 Vérifier l'existence de `Terminal.label()` ; sinon ajouter (champ JPA `label varchar(64)` + migration Flyway si table existe déjà)
- [ ] 3.2 `VerifyPublicTicketQueryHandler` — récupérer `terminal.label()` au lieu de masquer l'UUID
- [ ] 3.3 Si `label == null`, retourner `null` (pas de fallback UUID)

## 4. `payoutStatus` réel

- [ ] 4.1 Créer enum interne `PayoutStatus` ou conserver `String` (cohérence projet — préférence : enum sérialisé en string)
- [ ] 4.2 Logique de calcul dans `VerifyPublicTicketQueryHandler.computePayoutStatus(ticket)` :
  - `saleStatus == VOID || REJECTED` → `VOID`
  - `resultStatus == NOT_RESULTED` → `PENDING_DRAW`
  - `resultStatus == LOST` → `LOST`
  - `resultStatus == WON || OVERRIDDEN` :
    - `settlementStatus == SETTLED` → `WON_PAID`
    - `settlementStatus == UNSETTLED` → `WON_UNCLAIMED`
- [ ] 4.3 Si non visible (expired) → `EXPIRED`
- [ ] 4.4 Tests unitaires `PayoutStatusComputationTest` — matrice exhaustive (5 saleStatus × 4 resultStatus × 2 settlementStatus)

## 5. Mask `outletAddress`

- [ ] 5.1 `VerifyPublicTicketQueryHandler.maskAddress(Address)` retourne `MaskedAddress(a.city(), a.country())`
- [ ] 5.2 Si `address == null` → `MaskedAddress(null, null)` ou `null` (décision en review)
- [ ] 5.3 Tests : aucune valeur non-null hors `city/country` ne doit transiter par le DTO

## 6. Catch ciblé + observabilité

- [ ] 6.1 `try { lookup outlet } catch (DataAccessException | IllegalStateException e) { log.warn("Outlet enrichment failed publicCode={} message={}", code, e.getMessage()); meterRegistry.counter("tch_sales_verify_outlet_enrichment_failure_total", "tenant", tenantId.toString()).increment(); }`
- [ ] 6.2 Idem pour `resolveVisibilityDays` : catch ciblé, log WARN si fallback `14` utilisé, metric `tch_sales_verify_visibility_fallback_total`
- [ ] 6.3 Injection de `MeterRegistry` dans le handler
- [ ] 6.4 Tests : forcer une exception sur `outletReader.findById` (mock) → vérifier log WARN émis + metric incrémentée

## 7. Tests handler complets

- [ ] 7.1 `VerifyPublicTicketQueryHandlerTest` — 14 scenarios :
  - Public code inconnu → null (404 côté controller)
  - Public code valide ticket SOLD/NOT_RESULTED visible → `payoutStatus=PENDING_DRAW`
  - Public code valide ticket SOLD/WON/UNSETTLED → `payoutStatus=WON_UNCLAIMED`
  - Public code valide ticket SOLD/WON/SETTLED → `payoutStatus=WON_PAID`
  - Public code valide ticket SOLD/LOST → `payoutStatus=LOST`
  - Public code valide ticket VOID → `payoutStatus=VOID`
  - Public code valide ticket EXPIRED (au-delà visibility days) → `payoutStatus=EXPIRED`
  - 5 autres : address absente, terminal sans label, draw sans channel, settings catalog en erreur (fallback 14 + log WARN), enrichment outlet en erreur (log WARN + metric)
- [ ] 7.2 Vérifier sur tous les scenarios : aucun UUID interne dans le payload sérialisé

## 8. Documentation

- [ ] 8.1 `tchalanet-server/src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md` :
  - §3 (Modèle) — refonte description `TicketVerificationResult`
  - §9 — retirer les anomalies P0 traitées
- [ ] 8.2 `tchalanet-docs/docs/02-functional/flows/verify-ticket.md` :
  - Réécrire la section "Réponse — `TicketVerificationResult`" avec le nouveau shape
  - Réécrire le tableau "Sécurité — surfaces d'exposition publique" (colonnes `id`/`tenantId` → ❌ non exposé)
  - Ajouter la matrice des 6 valeurs `payoutStatus` avec exemples
- [ ] 8.3 `docs/decisions/sales-pipeline-decisions.md` (ou créer) — décisions D1-D6

## 9. Vérification finale

- [ ] 9.1 `./mvnw clean verify` → build vert + tous tests
- [ ] 9.2 Test e2e (manuel ou Postman) : `GET /public/tickets/verify/{publicCode}` → vérifier le JSON :
  - aucun `ticketId` / `drawId` / `address.id` / `address.tenantId`
  - `terminalLabel` est un alias humain (ex: "POS-001")
  - `payoutStatus` ∈ {PENDING_DRAW, WON_UNCLAIMED, WON_PAID, LOST, VOID, EXPIRED}
- [ ] 9.3 Vérifier les nouvelles metrics Prometheus exposées
- [ ] 9.4 CHANGELOG : `BREAKING (public API)` sur `TicketVerificationResult` shape + `payoutStatus` values
- [ ] 9.5 Coordonner front public pour adapter le parsing
