# Pagination — Canonical Server Rule (Tchalanet)

> Harmonisé avec `common.web.pagination` et conventions DTO (Request/Response), wrappers d’ID et enveloppe ApiResponse.

Dernière mise à jour: 2026-01-17

Ce document est la **source de vérité** pour la pagination côté serveur.  
Tout changement de règle de pagination DOIT être fait **uniquement ici**.

---

## Principe

- Les endpoints de liste **MUST** accepter un `@TchPaging(...) TchPageRequest`.
- Le mapping requête → query **MUST** dériver `page` et `size` via `pageReq.pageable()`.
- Les tris autorisés **MUST** être déclarés dans `@TchPaging(allowedSort=...)`.
- Le retour **MUST** être `ApiResponse<TchPage<T>>` (et non `Page<T>` de Spring).
- Les tailles par défaut et max **MUST** être bornées (ex: défaut=50, max=200).
- Convention DTO contrôleurs **MUST**: les types d’items exposés côté web se terminent par `Response`; les bodies d’entrée se terminent par `Request`.

## Exemple canonique

```java
@GetMapping
public ApiResponse<TchPage<TicketResponse>> list(
    @TchPaging(
      allowedSort = {"createdAt","status","ticketCode"},
      defaultSort = {"createdAt,DESC"}
    ) TchPageRequest pageReq,
    @CurrentContext TchRequestContext ctx
) {
  var page = pageReq.pageable().getPageNumber();
  var size = pageReq.pageable().getPageSize();
  var out = queryBus.send(new ListTicketsQuery(page, size));
  return ApiResponse.success(out);
}
```

Notes:

- Si l’endpoint accepte un body, celui-ci doit être un `XxxRequest` (ex: `FilterTicketsRequest`).
- Les items retournés dans la page doivent être un `XxxResponse` (ex: `TicketResponse`).

## Bonnes pratiques

- Toujours whitelister les colonnes de tri (`allowedSort`).
- Éviter les tris ambigus (multisort non requis par défaut).
- Uniformiser les noms de champs côté `Response` (camelCase, stables).
- Documenter la pagination dans Swagger (groupes et exemples).

## Anti-patterns (interdits)

- Retourner `Page<T>` (Spring) directement.
- Accepter `page`/`size` bruts sans `TchPageRequest`.
- Ne pas borner la taille max.
- Utiliser des DTO qui ne respectent pas les suffixes `Request` / `Response` dans les controllers.

---

## Defaults & configuration

- Taille par défaut = 50 (configurable via `common.web.pagination.defaultSize`).
- Taille max = 200 (configurable via `common.web.pagination.maxSize`).
- Les converters Spring (String -> Wrapper) doivent être actifs (`common.web.converter`).

## Liens techniques

- Converters ID wrappers: `common.web.converter.*`
- Types pagination: `common.web.pagination.TchPage`, `common.web.pagination.TchPageRequest`
- Enveloppe: `common.web.api.ApiResponse`

---

## Références liées

- Playbook (procédure) : `tchalanet-server/docs/PLAYBOOK.md` (sections Controller + API)
- Routing & scopes : `tchalanet-server/docs/ROUTING_AND_API_PATHS_V1.md`
- ApiResponse (enveloppe) : `tchalanet-server/docs/API_RESPONSE_STANDARDIZATION.md`

---

Note: Les fichiers `docs/conventions/pagination.md` et les mentions dans `web_api*.md` sont **dépréciés**; ils doivent pointer vers ce document.
