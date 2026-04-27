# Tchalanet — Pagination Conventions (Server)

## Status

**NORMATIVE**

---

## Objectif du document

Ce document définit **uniquement** le standard de pagination pour les endpoints REST :

1. les types canoniques (`TchPage<T>`, `TchPageRequest`),
2. la signature controller (`@TchPaging ... TchPageRequest`),
3. les règles de validation (size max, sort allowlist, defaults),
4. le contrat stable renvoyé au frontend.

Ce document **NE COUVRE PAS** :

- routing/scopes → voir `routing_and_path.md`
- enveloppe `ApiResponse<T>` → voir `api_response.md`
- conventions controllers/DTO → voir `web_api.md`
- détails persistence/query SQL → voir `persistence.md`

---

## 0) Principe canonique

- Toute liste paginée expose une pagination **offset-based** (page/size/sort).
- La forme **exposée** au client est toujours `TchPage<T>`.
- On **ne renvoie jamais** `org.springframework.data.domain.Page` au client.
- La pagination est appliquée côté query/reader (DB), jamais en mémoire.

---

## 1) Types canoniques

### 1.1 `TchPage<T>` (output)

`TchPage<T>` est le type de pagination stable consommé par le frontend.

Il contient typiquement :

- `items: List<T>`
- `page: int`
- `size: int`
- `total: long`
- `totalPages: int`
- `sort: List<String>` (représentation stable)

> Le shape exact est défini par l'implémentation `common.web.paging` (ou équivalent).  
> Ce document impose l'usage, pas les détails internes.

### 1.2 `TchPageRequest` (input)

`TchPageRequest` encapsule :

- `Pageable pageable()` (interne)
- champs raw (page, size, sort) après parsing/validation

---

## 2) Signature controller (MUST)

Tout endpoint listé/paginé **DOIT** :

- accepter `TchPageRequest` via l'annotation `@TchPaging`
- retourner `ApiResponse<TchPage<...>>`

**Exemple canonique** :

```java
@GetMapping
public ApiResponse<TchPage<TicketItemResponse>> list(
    @CurrentContext TchRequestContext ctx,
    @TchPaging(
        allowedSort = {"soldAt","status"},
        defaultSort = {"soldAt,desc"}
    ) TchPageRequest pageReq,
    @RequestParam(required = false) String q
) {
  var query = new ListTicketsQuery(ctx.effectiveTenantIdRequired(), q, pageReq.pageable());
  var page = queryBus.handle(query);
  return ApiResponse.success(page);
}
```

---

## 3) Paramètres HTTP (contrat)

Les paramètres attendus sont :

- `page` (int, 0-based)
- `size` (int)
- `sort` (repeatable) sous forme `field,asc|desc`

**Exemples** :

```
GET /tenant/tickets?page=0&size=20
GET /tenant/tickets?page=1&size=50&sort=soldAt,desc
GET /tenant/tickets?page=0&size=20&sort=status,asc&sort=soldAt,desc
```

---

## 4) Règles de validation (MUST)

### 4.1 Defaults

Si le client n'envoie rien :

- `page = 0`
- `size = DEFAULT_SIZE` (défini par le projet)
- `sort = defaultSort` (défini par `@TchPaging`)

### 4.2 Bornes

- `page` doit être `>= 0`
- `size` doit être `>= 1`
- `size` doit être `<= MAX_SIZE` (défini par le projet)

Si la validation échoue :

- renvoyer `ProblemDetail` (400) via `ProblemRest.badRequest(...)`

### 4.3 Sort allowlist (obligatoire)

Tout endpoint paginé **DOIT** :

- déclarer `allowedSort` (allowlist)
- déclarer `defaultSort`

Toute tentative de tri sur un champ non allowlisté :

- renvoie `ProblemDetail` (400)

**Raison** : éviter injections / queries non indexées / instabilité.

---

## 5) Côté Query/Handler (MUST)

Le controller transmet uniquement `pageReq.pageable()` au query model.

Le handler/reader applique la pagination en DB et retourne `TchPage<T>`.

**Interdit** :

- transformer `Pageable` en `Page` exposé directement au controller
- paginer en mémoire après avoir tout chargé

---

## 5.1 Implementation note: mapping from Spring `Page`

Dans l’implémentation actuelle, les readers/adapters peuvent produire un `org.springframework.data.domain.Page<A>` côté persistence, puis le convertir en `TchPage<B>` via un helper central.

- Helper : `com.tchalanet.server.common.web.paging.TchPages`
- Rôle : transformer `Page<A>` en `TchPage<B>` en appliquant un mapper

Exemple :

```java
var page = repo.findAll(spec, pageable);
return TchPages.map(page, mapper::toResponse);
```

## 6) Endpoints non paginés

Un endpoint peut être non paginé uniquement si :

- la cardinalité est strictement bornée (ex: <= 50 items), ou
- c'est un référentiel stable et petit (catalog), ou
- c'est un endpoint "summary".

Dans ce cas :

- pas de `@TchPaging`
- retourner une `List<T>` (toujours enveloppée en `ApiResponse` selon `api_response.md`)

---

## 7) Checklist avant PR (Pagination)

- [ ] Endpoint de liste utilise `@TchPaging` + `TchPageRequest`
- [ ] `allowedSort` défini (allowlist)
- [ ] `defaultSort` défini
- [ ] `size` borné (MAX_SIZE)
- [ ] Retour = `ApiResponse<TchPage<...>>`
- [ ] Pas de `Page<T>` Spring exposé
- [ ] Pagination appliquée en DB (pas en mémoire)

---

## Documents liés

- `web_api.md`
- `api_response.md`
- `routing_and_path.md`
