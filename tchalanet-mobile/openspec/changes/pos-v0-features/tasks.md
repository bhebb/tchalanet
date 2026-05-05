# Tasks — `pos-v0-features`

> Références : [proposal](proposal.md) · [design](design.md) · Specs [01](specs/bff-endpoints/spec.md) [02](specs/sales-multi-entry/spec.md) [03](specs/flutter-integration/spec.md) [04](specs/pos-screens/spec.md)

---

## 1. Vérification préalable (OQ-1)

- [ ] 1.1 Lire `pos-v0-foundation` — vérifier que `TicketPlacedEvent` contient bien `sessionId`; si absent, ajouter le champ au record et mettre à jour `SalesSessionTicketCountersListener` avant tout autre développement (OQ-1 / design D5)

---

## 2. Flyway migration — `core.sales`

- [ ] 2.1 Créer le fichier de migration Flyway (prochaine version disponible) avec : tables `ticket` et `ticket_entry` (schéma exact Spec 02), RLS via `BaseTenantEntity`, indexes `ux_ticket_code_per_tenant`, `ux_ticket_idempotency`, `ix_ticket_session`, `ix_ticket_status`, `ix_ticket_entry_ticket`, `ix_ticket_entry_draw`
- [ ] 2.2 Ajouter dans la même migration : tables `ticket_aud` et `ticket_entry_aud` (Envers) avec colonnes miroir + FK vers `revinfo(rev)`
- [ ] 2.3 Valider que `ddl-auto=validate` passe en local avec Testcontainers (`./mvnw test -pl :module-core-sales`)

---

## 3. Domain model — `core.sales`

- [ ] 3.1 Créer les typed IDs : `TicketId`, `TicketEntryId` (suivre `typed_ids.md`)
- [ ] 3.2 Créer l'entité JPA `TicketJpaEntity` (BaseTenantEntity, `@Audited`) avec toutes les colonnes Spec 02
- [ ] 3.3 Créer l'entité JPA `TicketEntryJpaEntity` (BaseTenantEntity, `@Audited`, `@ManyToOne → TicketJpaEntity`)
- [ ] 3.4 Créer le repository JPA `TicketJpaRepository` (queries par session, par code, par idempotencyKey)
- [ ] 3.5 Créer l'agrégat domain `Ticket` (record ou classe immuable) avec méthodes `approve(...)`, `cancel(...)`, `markPlaced()`
- [ ] 3.6 Créer `TicketEntry` (value object / record)
- [ ] 3.7 Créer `PlaceTicketCommand` (record avec `sessionId`, `List<EntryCommand>`, `smsOptin`, `customerPhone`, `idempotencyKey`)
- [ ] 3.8 Créer `ApproveBlockedTicketCommand` (record avec `ticketId`, `approverId`, `role`, `reason`)
- [ ] 3.9 Créer `CancelTicketCommand` (record avec `ticketId`, `reason`)
- [ ] 3.10 Créer `TicketPlacedEvent`, `TicketApprovedEvent`, `TicketCancelledEvent` (records avec tous les champs Spec 02, dont `sessionId` dans `TicketPlacedEvent`)

---

## 4. Command handlers — `core.sales`

- [ ] 4.1 Implémenter `PlaceTicketCommandHandler` :
  - validation session OPEN + propriété
  - validation de chaque entrée (draw OPEN, bet type valide, sélection canonicalisée, montant dans les limites settings)
  - calcul fees SMS
  - `LimitPolicyRuntimeService` → PLACED / PENDING_APPROVAL / BLOCKED (Spec 02 D4)
  - persistence atomique `ticket` + `ticket_entry`
  - after-commit : `TicketPlacedEvent` (si PLACED) ; SMS dispatch (si smsOptin)
  - idempotency check via `ux_ticket_idempotency` index
- [ ] 4.2 Implémenter `ApproveBlockedTicketCommandHandler` (validation rôle, `ticket.approve(...)`, after-commit `TicketApprovedEvent` + `TicketPlacedEvent`)
- [ ] 4.3 Implémenter `CancelTicketCommandHandler` (statuts valides, `sales.cancel.any` vs `sales.cancel.self` + fenêtre, after-commit `TicketCancelledEvent`)
- [ ] 4.4 Tests unitaires `PlaceTicketCommandHandlerTest` : scénarios PLACED, PENDING_APPROVAL, BLOCKED, idempotency replay, draw fermé rejeté
- [ ] 4.5 Tests unitaires `ApproveBlockedTicketCommandHandlerTest` : rôle insuffisant, rôle correct
- [ ] 4.6 Tests unitaires `CancelTicketCommandHandlerTest` : fenêtre expirée, admin bypass, statut invalide
- [ ] 4.7 Vérifier que les entrées audit (`SALE_PLACED`, `SALE_PENDING_APPROVAL`, `SALE_APPROVED`, `SALE_CANCELLED`) sont créées correctement, audit CRITICAL si `sales.cancel.any`

---

## 5. Read projections — `features.pos`

- [ ] 5.1 Implémenter `SalesSessionTicketCountersListener` : sur `TicketPlacedEvent` → incrémenter `ticketCount`, `salesDayHtg`, `avgTicketHtg` dans `sales_session_counters` (after-commit, idempotent via `ProcessedEventPort`)
- [ ] 5.2 Implémenter `DashboardDrawStatsListener` : sur `TicketPlacedEvent` → mettre à jour ventes du jour par `drawId`
- [ ] 5.3 Implémenter `RecentResultsProjectionListener` : sur `DrawSettledEvent` → upsert résultats récents (top 4)
- [ ] 5.4 Implémenter `LastTicketProjectionListener` : sur `TicketPlacedEvent` → stocker le dernier ticket par agent

---

## 6. BFF — `features.pos` (backend)

- [ ] 6.1 Créer la structure de package `features/pos/infra/web/api/`, `features/pos/application/`, `features/pos/infra/web/model/`
- [ ] 6.2 Implémenter `PosBootstrapController` + `PosBootstrapService` (B1 : user + outlet + terminal + currentSession + uiHints ; provisioning VIRTUAL si `X-Tch-Platform: MOBILE`)
- [ ] 6.3 Implémenter `PosSessionBffController` + `PosSessionBffService` (B2 session/open, B3 close/preview, B4 session/close)
- [ ] 6.4 Implémenter `PosDashboardController` + `PosDashboardService` (B5 dashboard + PARTIAL handling + `marginPct` omis si pas de permission)
- [ ] 6.5 Implémenter `PosSaleBffController` + `PosSaleBffService` (B6 sale/context, B7 sale/place, B8 approve, B9 cancel, B10 reprint, B11 ticket detail)
  - B7 : Idempotency-Key header obligatoire (400 si absent)
  - B7 : mapper résultat PLACED / PENDING_APPROVAL avec `block` + `approval` sections
  - B10 / B17 : `PRINTER_NOT_AVAILABLE` sur terminal VIRTUAL
- [ ] 6.6 Implémenter `PosHistoryController` + `PosHistoryService` (B12 history paginée avec filtres)
- [ ] 6.7 Implémenter `PosResultsController` + `PosResultsService` (B13 results + marge conditionnelle)
- [ ] 6.8 Implémenter `PosSettingsBffController` + `PosSettingsBffService` (B14 GET, B15 PUT, DELETE settings, B16 sync/trigger, B17 print/test)
- [ ] 6.9 Implémenter `PosOpsController` + `PosOpsService` (opérations ops si nécessaire)
- [ ] 6.10 Mapper tous les codes d'erreur domaine → `application/problem+json` avec `code` (liste canonique Spec 01)
- [ ] 6.11 Tests MockMvc pour tous les controllers (happy path + principal error codes)

---

## 7. Cache BFF — `features.pos`

- [ ] 7.1 Déclarer les 5 caches nommés dans la configuration Spring Cache (`pos.bff.dashboard`, `pos.bff.sale_context`, `pos.bff.ticket_detail`, `pos.bff.results`, `pos.bff.settings`)
- [ ] 7.2 Annoter les méthodes de service concernées avec `@Cacheable` / `@CacheEvict`
- [ ] 7.3 Implémenter les listeners d'éviction after-commit : `TicketPlacedEvent` → évict `pos.bff.dashboard` ; `DrawSettledEvent` → évict `pos.bff.results` ; `TicketCancelledEvent` / `TicketApprovedEvent` → évict `pos.bff.ticket_detail` ; `SettingChangedEvent` → évict `pos.bff.settings` + `pos.bff.sale_context` ; `DrawClosedEvent` → évict `pos.bff.sale_context`

---

## 8. OpenAPI

- [ ] 8.1 Vérifier que les 17 endpoints sont documentés dans Springdoc (annotations `@Operation`, `@ApiResponse`)
- [ ] 8.2 Vérifier que les DTOs de réponse utilisent uniquement des `String` pour les montants (`"14250.00"`)

---

## 9. Démantèlement app Ionic/Angular + Bootstrap app Flutter

### 9a. Suppression de l'app Ionic/Angular existante

> **Contexte** : Nx ne gère pas Flutter. Le pattern du monorepo est que les projets non-Nx vivent à la racine (`tchalanet-server/`, `tchalanet-infra/`, `tchalanet-edge-service/`, `tchalanet-docs/`). L'app Flutter sera créée dans `tchalanet-mobile/` à la racine — `apps/` est réservé aux projets Nx.

- [ ] 9a.1 Supprimer le répertoire `apps/tchalanet-mobile/` en entier (code Angular/Ionic + tous les fichiers de config Nx/Vite/Ionic)
- [ ] 9a.2 Supprimer le répertoire `apps/tchalanet-mobile-e2e/` (Playwright couplé à l'app Ionic)
- [ ] 9a.3 Retirer du `package.json` racine les dépendances Ionic/Capacitor : `@ionic/angular`, `@ionic/pwa-elements`, `ionicons`, `@nxext/ionic-angular`, `@nxext/capacitor`, `@capacitor/core`, `@capacitor/cli`, `@capacitor/android`, `@capacitor/ios`, `@capacitor/camera`, `@capacitor/filesystem`, `@capacitor/preferences`
- [ ] 9a.4 Lancer `pnpm install` pour mettre à jour `pnpm-lock.yaml`
- [ ] 9a.5 Vérifier `nx show projects` : `tchalanet-mobile` et `tchalanet-mobile-e2e` ne doivent plus apparaître
- [ ] 9a.6 Mettre à jour `AGENTS.md` (racine) : référence `apps/tchalanet-mobile/CLAUDE.md` → `tchalanet-mobile/CLAUDE.md` dans la section 4 et l'index rapide
- [ ] 9a.7 Mettre à jour `CLAUDE.md` (racine) : idem, tableau des sous-projets

### 9b. Bootstrap app Flutter dans `tchalanet-mobile/` (racine du monorepo)

- [ ] 9b.1 Créer et scaffolder l'app Flutter à la racine :

  ```bash
  cd /path/to/tchalanet
  flutter create tchalanet-mobile --org com.tchalanet \
    --project-name tchalanet_pos --platforms android,ios --template app
  ```

  Vérifier que `flutter run` démarre sans erreur sur émulateur Android.

- [ ] 9b.2 Ajouter `tchalanet-mobile/CLAUDE.md` reprenant le contenu de l'ancien `apps/tchalanet-mobile/CLAUDE.md` (stack Flutter, règles, commandes) — sans aucune référence Ionic/Angular

- [ ] 9b.3 Configurer `pubspec.yaml` avec les dépendances fondamentales :

  | Package                                      | Rôle                                          |
  | -------------------------------------------- | --------------------------------------------- |
  | `flutter_riverpod`                           | State management                              |
  | `riverpod_annotation` + `riverpod_generator` | Code gen providers                            |
  | `go_router`                                  | Routing déclaratif                            |
  | `dio`                                        | HTTP client                                   |
  | `flutter_secure_storage`                     | Token Keycloak (Keychain/Keystore)            |
  | `sqflite` + `path`                           | DB locale (draft entries, cache offline)      |
  | `decimal`                                    | Montants monétaires sans perte de précision   |
  | `intl`                                       | Formatage dates/montants localisés            |
  | `envied` + `build_runner`                    | Variables d'env par flavor (secrets compilés) |
  | `freezed` + `freezed_annotation`             | Value objects / DTOs immuables                |
  | `json_serializable`                          | Sérialisation JSON des DTOs                   |
  | `flutter_localizations` + `intl`             | i18n fr/en/ht                                 |

- [ ] 9b.4 Mettre en place la **structure de dossiers** dans `tchalanet-mobile/lib/` :

  ```
  lib/
  ├── core/
  │   ├── http/           # ApiClient, interceptors (auth, platform, idempotency)
  │   ├── router/         # router.dart, shell_screen.dart, navigation guards
  │   ├── storage/        # secure_storage.dart, local_db.dart (sqflite)
  │   ├── error/          # ApiError, error_mapper.dart (codes → messages FR)
  │   └── providers.dart  # providers globaux exportés
  ├── features/
  │   ├── auth/           # S1 Login + boot flow
  │   ├── session/        # S2 open, S9 close
  │   ├── dashboard/      # S3
  │   ├── sale/           # S4 + S5 approval modal + S6 ticket detail
  │   ├── history/        # S7
  │   ├── results/        # S8
  │   └── settings/       # S10
  ├── shared/
  │   ├── widgets/        # session_banner.dart, bottom_nav.dart, countdown_timer.dart
  │   ├── models/         # DTOs partagés (AppSession, UserDto, OutletDto…)
  │   └── utils/          # currency_formatter.dart, date_utils.dart
  └── main.dart           # entrée (ProviderScope → MaterialApp.router)
  ```

- [ ] 9b.5 Configurer les **3 build flavors** (`dev`, `staging`, `prod`) :

  - Créer `tchalanet-mobile/lib/env/env_dev.dart`, `env_staging.dart`, `env_prod.dart` via `envied` avec `apiBaseUrl` et `keycloakBaseUrl`
  - Configurer `tchalanet-mobile/android/app/build.gradle` avec `flavorDimensions` et `productFlavors` (dev/staging/prod)
  - Vérifier : `flutter run --flavor dev` démarre et utilise `https://dev-api.tchalanet.com`

- [ ] 9b.6 Implémenter `ApiClient` (`tchalanet-mobile/lib/core/http/api_client.dart`) avec Dio + 3 intercepteurs :

  - `AuthInterceptor` : injecte `Authorization: Bearer <access_token>` ; sur 401 tente un refresh Keycloak avant de retrier ; sinon logout
  - `PlatformInterceptor` : injecte `X-Tch-Platform: MOBILE` (ou `TERMINAL` selon flavor)
  - `IdempotencyInterceptor` : injecte `Idempotency-Key: <uuid>` uniquement sur les endpoints POST marqués (liste configurée dans `api_client.dart`)

- [ ] 9b.7 Implémenter `SecureStorageService` (`tchalanet-mobile/lib/core/storage/secure_storage.dart`) :

  - `saveRefreshToken(String token)` / `readRefreshToken()` / `clearAll()` via `flutter_secure_storage`

- [ ] 9b.8 Implémenter `LocalDb` (`tchalanet-mobile/lib/core/storage/local_db.dart`) avec `sqflite` :

  - Table `draft_entries` : `id INTEGER PK, draw_id TEXT, bet_type_code TEXT, selection TEXT, amount_htg TEXT, created_at INTEGER`
  - Table `pending_close` : `id INTEGER PK, closing_amount TEXT, saved_at INTEGER` (préservation montant fermeture en cas de perte réseau)
  - Migrations versionnées via `onUpgrade`

- [ ] 9b.9 Implémenter `AppSessionNotifier` (`tchalanet-mobile/lib/core/providers.dart`) :

  - `StateNotifier<AppSession?>` Riverpod
  - Méthodes : `setFromBootstrap(BootstrapResponse)`, `setSession(SalesSessionDto)`, `clearSession()`, `updateCounters(...)`
  - Provider global : `appSessionProvider`

- [ ] 9b.10 Configurer **GoRouter** (`tchalanet-mobile/lib/core/router/router.dart`) :

  - Shell route pour bottom nav (4 tabs : Sales `/`, Reports `/reports`, History `/history`, Settings `/settings`)
  - Routes S1–S10 avec leurs paths
  - `redirect` guard : si `appSessionProvider == null` et route protégée → `/login` ; si session null et route tab (sauf `/settings`) → `/no-session`
  - Vérifier la navigation S1 → S2 → S3 sur émulateur

- [ ] 9b.11 Wiring final dans `tchalanet-mobile/lib/main.dart` :
  - `ProviderScope` wrapping `MaterialApp.router(routerConfig: router)`
  - Thème Material 3 (couleurs via `ColorScheme.fromSeed`, pas de couleurs hardcodées)
  - `supportedLocales: [fr, en, ht]` + `localizationsDelegates`
  - Vérifier `flutter analyze` : 0 erreur, 0 warning

---

## 10. Flutter — Repositories BFF

- [ ] 10.1 `PosBootstrapRepository` → `GET /bff/bootstrap`
- [ ] 10.2 `PosSessionRepository` → B2 open, B3 close/preview, B4 close
- [ ] 10.3 `PosDashboardRepository` → B5 dashboard
- [ ] 10.4 `PosSaleRepository` → B6 context, B7 place, B8 approve, B9 cancel, B10 reprint, B11 detail
- [ ] 10.5 `PosHistoryRepository` → B12 history
- [ ] 10.6 `PosResultsRepository` → B13 results
- [ ] 10.7 `PosSettingsRepository` → B14 GET, B15 PUT, DELETE, B16 sync, B17 print/test
- [ ] 10.8 Mapper tous les codes `problem+json` → messages français (table Spec 03) via `error_mapper.dart` ; fallback "Une erreur inattendue s'est produite."

---

## 11. Flutter — Screens S1 à S5

- [ ] 11.1 **S1 Login** : formulaire email/password, toggle show/hide, "Mot de passe oublié ?" → browser, bouton désactivé jusqu'à saisie complète, app boot flow complet (bootstrap → routing)
- [ ] 11.2 **S2 Aucune session active** : cards read-only outlet/terminal/user, input fond de caisse + chips rapides (+500/+1000/+2000/+5000), bouton jamais désactivé (0 autorisé), `POST session/open` + navigation S3
- [ ] 11.3 **S3 Dashboard** : header + session banner + section tirages en cours (horizontal scroll, countdown color-coded) + KPI grid 2×2 + résultats récents + dernier ticket ; refresh on focus + every 30 s ; gestion PARTIAL response
- [ ] 11.4 **S4 Nouvelle vente** : `DraftEntryNotifier`, sélection tirage, chips bet type, input numéro + montant, chips rapides, "Ajouter au ticket", liste entrées + Vider, footer sticky total + Valider ; `POST sale/place` → PLACED (reset + S3) ou PENDING_APPROVAL (→ S5) ou SALE_BLOCKED (toast) ou IDEMPOTENCY_PAYLOAD_MISMATCH
- [ ] 11.5 **S5 Approbation (modal)** : titre + résumé limite + reason ; `callerCanSelfApprove` → bouton Approuver + textarea ; sinon → message superviseur ; `POST sale/{id}/approve`

---

## 12. Flutter — Screens S6 à S10

- [ ] 12.1 **S6 Ticket detail** : header + status chip + metadata + entries list + total + fees + customer phone/SMS + timeline + action buttons conditionnels (canCancel, canReprint, canApprove) ; `POST cancel` (confirm modal) ; `POST reprint` → printer driver
- [ ] 12.2 **S7 Historique** : search input + date picker + filter chips (session/aujourd'hui/7 jours + lottery + status) + liste tickets cards (tap → S6) + load more on scroll ; empty state "Aucun ticket pour ces filtres."
- [ ] 12.3 **S8 Résultats** : tabs Aujourd'hui/Hier/Custom + filter chips lottery + draw cards (winning numbers big circles + stats Ventes/Payé/Marge) ; marge cachée si pas `sales.results.margin.read` ; empty state "Pas de résultats pour cette période."
- [ ] 12.4 **S9 Fermer la session** : mount `GET close/preview` + récap read-only + input montant final + alerte écart (orange, avec note) + confirm modal irréversible + `POST session/close` → S2 ; préserver montant saisi si connexion perdue
- [ ] 12.5 **S10 Paramètres** : sections A/B/C/D ; section D masquée si `terminal.kind != PHYSICAL` ; champs éditables avec PUT ; 🔒 + tooltip si `editable = false` ; lien Réinitialiser → DELETE ; "Synchroniser maintenant" → B16 ; "Imprimer une page de test" → B17

---

## 13. Flutter — Comportements transverses

- [ ] 13.1 Bottom navigation : 4 onglets (Sales / Reports / History / Settings) ; onglets désactivés (grisés, non-interactifs) quand pas de session active sauf Settings
- [ ] 13.2 Session banner global : affiché sur S3–S10, absent sur S1 et S2 ; "Fermer" → S9
- [ ] 13.3 Network resilience : détection perte réseau → banner "Connexion perdue, les ventes sont temporairement indisponibles." ; refresh dashboard au reconnect
- [ ] 13.4 Rendu montants via `package:decimal` : parse string → Decimal → affichage `"14 250,00 HTG"` (jamais `double`, jamais `$`)
- [ ] 13.5 Countdown timers : calcul vs device clock, color-coding (> 30 min gris / 5–30 min ambre / < 5 min rouge / <= 0 "Fermé" + card disabled)
- [ ] 13.6 `PrinterService` : stub abstraction (interface) ; implémentation concrète pour Motorola POS définie dans tâche dédiée séparée (OQ-2)

---

## 14. Tests & validation finale

- [ ] 14.1 Tests d'intégration backend (`@SpringBootTest` + Testcontainers) : scénario complet place → approve → cancel
- [ ] 14.2 Tests MockMvc BFF : tous les 17 endpoints (happy path + codes d'erreur canoniques)
- [ ] 14.3 Tests ArchUnit : vérifier que `features.pos` ne contient pas de logique domaine ; `core.sales` ne dépend pas de `features`
- [ ] 14.4 Smoke test manuel Flutter `dev` flavor : S1 → S2 → S3 → S4 (place ticket) → S6 → S9 (close session)
- [ ] 14.5 Vérifier `ddl-auto=validate` passe sur staging après migration
- [ ] 14.6 Activer feature flag `pos.bff.enabled` sur staging (Unleash)
