# Tasks — web-async-state-resource-v1

## 1. Primitives (`libs/api` + `libs/web/async`)

- [ ] `TchBackendClient.getResource<T>()` / `getPageResource<T>()` — rxResource interne sur `get()`/`getPage()`, injector capturé, lazy quand `request()` renvoie `undefined` (+ tests : params réactifs, annulation, unwrap ApiResponse)
- [ ] Générer la lib Nx `web-async` (alias `@tch/web/async`), tags/boundaries alignés (`web/async → api, web/errors, ui/*`)
- [ ] `resourceErrorVm(res, source)` + tests (ProblemDetail, erreur non-HTTP, null)
- [ ] `tchMutation()` + tests (pending global/par clé, feedback succès/erreur, onSuccess, clearFeedback)
- [ ] `server-field-message` centralisé (une copie, supprime les helpers privés des pages au fil des migrations)
- [ ] `<tch-async-view>` + `tchAsyncReady` + tests (loading/error+retry+traceId/empty/ready, prédicat isEmpty)
- [ ] `tch-async-view` : état `reloading` (données visibles + barre discrète, jamais de blanchiment) + anti-flash (délai ~300ms, min ~500ms) + slot `loading` surchargeable (skeleton futur)
- [ ] `tch-pagination` dans `libs/ui/console` : « N–M sur Total », prev/next, sélecteur de taille (10/20/50), piloté par `page`/`size` URL (+ tests)
- [ ] `pnpm nx run-many -t build,lint,test` sur la lib + apps affectées

## 2. Pilote setup

- [ ] Migrer `admin/setup/pages/settings/admin-config.page` : rxResource par section + 2 tchMutation
- [ ] Zéro changement visuel (vérifier states loading/error/empty/success + e2e setup existants)
- [ ] Mesure avant/après dans la PR (signals, lignes)

## 3. Pilote overview

- [ ] Migrer `admin/draws/pages/overview/admin-generated-draws.page` : resource paramétré URL + mutations lifecycle + drawer
- [ ] Migrer les filtres locaux (datePreset/statusFilter/searchQuery) vers les query params standard (`q`, `status`, + préfixés) — plus aucun filtre en signal local
- [ ] Remplacer le footer de pagination local par `tch-pagination`
- [ ] Pending par ligne sur les actions draw ; reload après action (reloading : pas de blanchiment)
- [ ] e2e draws existants verts

## 4. Codification

- [ ] Playbook §1.3 : le standard d'état de page devient resources + tchMutation (+ squelette tch-async-view §1.4)
- [ ] Playbook §2 (archétype liste) : pagination `tch-pagination`, params URL standard (`q`/`status`/`sort`/`page`/`size`), tri par défaut explicite, règle « jamais de blanchiment au reload »
- [ ] Playbook §1.9 : ligne resource() mise à jour (décision prise, référence à ce change)
- [ ] `state-management.md` : section 3.2 réécrite (rxResource par défaut, plus de Subject+switchMap pour les nouveaux écrans)
- [ ] `AGENTS.md` : ligne de routage état async → `@tch/web/async` + références angular-developer (`resource.md`)
- [ ] `openspec validate --strict` puis archive du change
