# Tasks — Phase 1 : Policy + Inventory

**Règle :** Validation humaine avant toute modification de fichier source.  
**Ne pas toucher :** `ai-agents/`, `setup-ai-agents-slice-first/`

---

## 1. Audit documentaire initial ✅

- [x] Lister tous les dossiers sous `openspec/changes/` et classifier : livré | actif | obsolète
- [x] Lister tous les dossiers sous `tchalanet-docs/docs/` et identifier les orphelins
- [x] Relancer `scripts/docs/inventory-docs.py` pour un inventaire frais
- [x] Produire tableau : dossier / statut / action recommandée

**Résultats :**
- 1090 fichiers Markdown — 211 UNKNOWN, 54 DUPLICATE, 236 ARCHIVE
- Orphelins `tchalanet-docs` : `00-audit/`, `02-domains/`, `03-apps/`, `05-decisions/`, `99-reference/`
- 12 packs fantômes dans `openspec/context/00-index.md`
- 0 `DOMAIN_*.md` near-code dans `tchalanet-server/src/`

## 2. Mettre à jour `doc-policy.md` ✅

- [x] Ouvrir `tchalanet-docs/docs/00-guidelines/doc-policy.md`
- [x] Ajouter hiérarchie de vérité documentaire (8 niveaux)
- [x] Ajouter section "Cycle de vie OpenSpec" avec checklist d'extraction
- [x] Ajouter règles de contenu par type (DOMAIN_*, conventions, tchalanet-docs, openspec/context)
- [x] Ajouter "Documentation Ownership" par projet
- [x] Ajouter severity policy (errors vs warnings) et PR Checklist docs
- [x] Corriger `.claude/skills/` → `.agents/skills/`
- [x] Bump version 2.0.0 + date 2026-05-30

## 3. Réécrire `archive-policy.md` ✅

- [x] Ouvrir `tchalanet-docs/docs/06-openspec/archive-policy.md`
- [x] Interdire `openspec/changes/archive/`
- [x] Documenter la checklist d'extraction avant suppression
- [x] Bump version 2.0.0

## 4. Nettoyer `openspec/context/00-index.md` ✅

- [x] Lister chaque pack référencé (14 packs référencés)
- [x] Vérifier existence de chaque fichier
- [x] Retirer les 12 entrées fantômes du root `openspec/context/`
- [x] Pointer vers les packs réels dans `tchalanet-server/openspec/context/` (19 packs)
- [x] Ajouter table "Packs manquants" (9 à créer en Phase 3)
- [x] Ajouter note : "Router IA — pointe vers fichiers canoniques, ne recopie pas les règles"

## 5. Corriger les liens cassés ✅

- [x] `DOCUMENTATION.md` (root) : `.claude/skills/` → `.agents/skills/`, bump 2.1.0
- [x] `doc-policy.md` : table de référence `.claude/skills/` → `.agents/skills/`
- Fichiers `99-reference/` : générés automatiquement — à corriger via le script (hors scope Phase 1)

## 6. Audit lecture-seule — conventions server ✅

**ARCHITECTURE.md** : 976 lignes, bien structuré, couvre les 5 couches. À jour.

**Conventions existantes** (`tchalanet-server/docs/conventions/`) :

| Convention | Fichier | Statut |
|---|---|---|
| Command/Query handlers | `command_query_handlers.md` | ✅ existe |
| Typed IDs | `typed_ids.md` | ✅ existe |
| Event model | `event_model.md` | ✅ existe |
| Testing | `testing.md` | ✅ existe |
| Idempotency | `idempotency.md` | ✅ existe |
| Inter-domain calls | `inter_domain_calls.md` | ✅ existe |
| Bus | `bus.md` | ✅ existe |
| Cache | `cache.md` | ✅ existe |
| Batch | `batch.md` | ✅ existe |
| Timezone | `timezone.md` | ✅ existe |
| Security permissions | `security_permissions.md` | ✅ existe |
| Offline sync claim | `offline-sync-claim.md` | ✅ existe |
| Ops force flag | `ops_force_flag.md` | ✅ existe |
| User contexte op. | `user-contexte-operational.md` | ✅ existe |
| Persistence | `persistence/persistence.md` | ✅ existe |
| RLS | `persistence/rls.md` | ✅ existe |
| JPA entities | `persistence/jpa_entities.md` | ✅ existe |
| Audit persistence | `persistence/audit.md` | ✅ existe |
| Sensitive JPA | `persistence/sensitive_jpa_updates.md` | ✅ existe |
| API response | `api/api_response.md` | ✅ existe |
| Web API | `api/web_api.md` | ✅ existe |
| Routing & path | `api/routing_and_path.md` | ✅ existe |
| Pagination | `api/pagination.md` | ✅ existe |
| Request context | `api/request_context_usage.md` | ✅ existe |
| Spring Batch 6 | `batch/spring_batch_6.md` | ✅ existe |
| DOMAIN_*.md near-code | `src/**/DOMAIN_*.md` | ❌ **0 fichiers** |
| Clean architecture | `clean_architecture.md` | ✅ existe |

**Gaps identifiés :**
- ❌ Aucun `DOMAIN_*.md` near-code — règles métier centralisées dans `tchalanet-docs` uniquement (Phase 3)
- ⚠️ `NAMING.md` existe mais séparé des conventions/ — à évaluer en Phase 3

## 7. Audit flows existants ✅

**Flows existants** (`tchalanet-docs/docs/02-functional/flows/`) :
- `sell-ticket.md` ✅
- `verify-ticket.md` ✅
- `draw-execution.md` ✅

**Flows manquants (TODO Phase 3) :**
- Payout terrain (vérification POS → décaissement)
- Offline sync (agent terrain → sync serveur)
- Session cashier (ouverture → clôture → réconciliation)
- Tenant onboarding (création → configuration → activation)
- Ledger recharge (crédit → validation → disponibilité)
- Public results pipeline (tirage → diffusion publique)
