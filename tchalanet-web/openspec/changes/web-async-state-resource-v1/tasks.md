# Tasks — web-async-state-resource-v1

## 1. Primitives (`libs/api` + `libs/web/async`)

- [x] `TchBackendClient.getResource<T>()` / `getPageResource<T>()` — rxResource interne sur `get()`/`getPage()`, injector capturé, lazy quand `request()` renvoie `undefined` (+ tests : params réactifs, annulation, unwrap ApiResponse)
- [x] Générer la lib Nx `web-async` (alias `@tch/web/async`), tags/boundaries alignés (`web/async → api, web/errors, ui/*`)
- [x] `resourceErrorVm(res, source)` + tests (ProblemDetail, erreur non-HTTP, null)
- [x] `tchMutation()` + tests (pending global/par clé, feedback succès/erreur, onSuccess, clearFeedback, **double-submit no-op**, hook `idempotency.keyFactory` — défaut `crypto.randomUUID()`)
- [x] `server-field-message` centralisé (une copie, supprime les helpers privés des pages au fil des migrations)
- [x] `<tch-async-view>` + `tchAsyncReady` + tests (loading/error+retry+traceId/empty/ready, prédicat isEmpty)
- [x] `tch-async-view` : état `reloading` (données visibles + barre discrète, jamais de blanchiment) + anti-flash (délai ~300ms, min ~500ms) + slot `loading` surchargeable (skeleton futur)
- [x] `tch-pagination` dans `libs/ui/console` : « N–M sur Total », prev/next, sélecteur de taille (10/20/50), piloté par `page`/`size` URL (+ tests)
- [x] `pnpm nx run-many -t build,lint,test` sur la lib + apps affectées

## 2. Pilote setup

- [x] Migrer `admin/setup/pages/settings/admin-config.page` : resource + 2 tchMutation (+ hook onError ajouté à tchMutation pour le mapping des erreurs serveur par champ — besoin récurrent des archétypes C/D)
- [x] Zéro changement visuel (mêmes états loading/error/notice/champs ; build dev + tests OK ; pas d'e2e setup existant — seul smoke.spec côté admin)
- [x] Matrice de couverture (design §7) : couverte par les tests unitaires de tch-async-view/tchMutation (stale, erreurs, double-clic) ; le pending par ligne sera exercé par le pilote overview
- [x] Mesure avant/après dans la PR (signals, lignes)

## 3. Pilote overview

- [ ] Migrer `admin/draws/pages/overview/admin-generated-draws.page` : resource paramétré URL + mutations lifecycle + drawer
- [ ] Migrer les filtres locaux (datePreset/statusFilter/searchQuery) vers les query params standard (`q`, `status`, + préfixés) — plus aucun filtre en signal local
- [ ] Remplacer le footer de pagination local par `tch-pagination`
- [ ] Pending par ligne sur les actions draw ; reload après action (reloading : pas de blanchiment)
- [ ] e2e draws existants verts

## 4. Codification

- [x] Playbook §1.3 : le standard de lecture devient resource (créé par le client) + §1.5 tchMutation
- [x] Playbook §1.4 : squelette = `tch-async-view` ; règle ready = `@if (resource.value(); as vm)` (jamais `let-vm`)
- [x] Playbook §2 (archétype liste) : `tch-pagination`, params URL standard, helpers `@tch/web/async`, tri par défaut explicite, « jamais de blanchiment au reload »
- [x] Playbook §1.9 + §8 (table des briques) + §9 (anti-patterns) alignés
- [x] Helpers URL partagés `@tch/web/async` (`numberParam`/`dateParam`/`textParam`/`enumParam`) + tests — dédup des 4 pages qui les recopiaient
- [x] `state-management.md` §3.2 réécrite (resource par défaut, plus de Subject+switchMap)
- [x] `AGENTS.md` : routage état async → `@tch/web/async` + Key libs
- [ ] Adoption des helpers URL dans les pages existantes (draws, tickets, catalog×2) — fil de l'eau
- [ ] `openspec validate --strict` puis archive du change (après merge des pilotes #214/#215)
