# PROJECT: TCHALANET — Multi-Tenant Lottery & Borlette Platform

# ARCHITECTURE → Backend: Java 21 + Spring Boot + Hexagonal Architecture

# Frontend: Angular 20 + Nx + Angular Material + CSS vars theme system

====================================================
🔵 CONTEXTE GÉNÉRAL
====================================================
Tchalanet est une plateforme multi-tenant pour opérateurs de loterie/borlette :

- page publique
- dashboard super admin (plateforme)
- dashboard tenant admin
- dashboard vendeur/caissier
- API internes
- batchs pour tirages et résultats US
- intégration POS + sessions vendeur
- thèmes dynamiques tenant-aware

Backend = clean architecture / hexagonal ports & adapters.
Frontend = Angular 20 mobile-first, theming basé sur design tokens + CSS vars.

====================================================
🔵 BACKEND – PRINCIPES À RESPECTER
====================================================
Langage: Java 21  
Framework: Spring Boot 3.x  
BDD: PostgreSQL + RLS + Flyway  
Architecture: Hexagonal (Ports IN/OUT + Adapters)  
DDD léger: domain model + use case + persistence adapters.

Règles backend obligatoires:

- Multi-tenant strict via tenant_id (ou tenant_code) + Row Level Security
- Soft delete (deleted_at)
- Audit columns: created_at/by, updated_at/by
- Flyway pour migrations (structure + seed)
- Entités immuables (records) ou @Entity minimal
- Use cases = classes de service dans application.service
- Ports = interfaces dans application.port.[in|out]
- Adapters = impl dans infra.persistence ou infra.external
- Validation via Spring Validation + records DTO

====================================================
🔵 MODELES MÉTIERS BACKEND
====================================================
Jeux: un seul game global "BORLETTE_BASE".
Variantes définies dans tenant_game.flags:
MATCH1, MATCH2, MATCH3
LOTTO3, LOTTO4, LOTTO5
MARIAGE
Rules:
LAST2_MATCH_RANK, EXACT_N_DIGITS, MARRIAGE_ANY_ORDER.

Tirages:

- draw_channel = catalogue plateforme (global, pas de tenant_id)
- tenant_draw_channel = activation des tirages par tenant
- draw = instances planifiées pour chaque tenant (générées par batch)

Sources résultats:

- FL_MID, FL_EVE (Florida Lottery)
- NY_MID, NY_EVE (New York Lottery)
- HTI_DAY (manuel)
  Ports: ExternalDrawResultPort
  Adapters:
  - UsOfficialLotteryAdapter (NY & Florida)
  - FakeExternalDrawResultAdapter

Sessions vendeur:

- cash_session = indépendante des tirages
- lien ticket.session_id pour stats

====================================================
🔵 FRONTEND – PRINCIPES À RESPECTER
====================================================
Framework: Angular 20 (signals, @for, @if)
Build: Nx monorepo
UI: Angular Material 17 + tokens CSS
Theming:

- Base tokens SCSS → runtime CSS vars par tenant
- Light/dark + presets (tchalanet, violet-green, etc.)
- Header public: mobile-first, CTA, search icon, language/theme chips
- Widgets rendus via WidgetRenderer
- Pages construites avec PageModel depuis le backend

Navigation:

- public: home, verify ticket, pricing, about
- private: dashboards selon rôle (super admin, tenant admin, vendeur)

Composants:

- ChangeDetectionStrategy.OnPush partout
- Signals pour les states
- No hex colors dans les composants → toujours CSS vars :root / component vars

====================================================
🔵 OBJECTIFS À PRODUIRE
====================================================
Copilot doit produire :

- entités backend Java + ports + services + adapters
- Flyway migrations (structure et seeds)
- SQL pour tenants, jeux, tirages, variantes
- endpoints REST multi-tenant sécurisés (Keycloak)
- adapters external API (NY/FL)
- batchs (GenerateUpcomingDraws, FetchExternalResults)
- controllers publics (PageModel)

Frontend:

- composants Angular Material responsives
- services avec HttpClient
- theming CSS vars
- pages publiques et privées
- WidgetRenderer pour sections dynamiques

====================================================
🔵 STYLE & QUALITÉ
====================================================
Toujours produire :

- code clair, pur hexagonal, sans logique dans contrôleurs
- use cases isolés
- records pour DTO
- repos Spring Data avec projection si possible
- tests unitaires sur domain + use cases
- pas de logique métier dans adapter DB
- Angular components minimalistes + OnPush + Signals

====================================================
🔵 À FAIRE PAR DÉFAUT
====================================================
Copilot doit :

- suggérer la bonne structure hexagonale
- créer les bons packages
- ajouter les interfaces ports
- créer les adapters NY/FL
- générer les seeds SQL cohérents
- générer la page publique (PageModelController)
- mettre les bonnes routes Angular par rôle
