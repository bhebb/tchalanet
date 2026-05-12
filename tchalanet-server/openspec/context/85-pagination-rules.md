# OpenSpec — Pagination Rules aligned to Modulith (85)

> Status: NORMATIVE

## 1. HTTP pagination

Paginated HTTP endpoints use `TchPageRequest` input and `TchPage<T>` output.

## 2. Ownership

Pagination primitives live in `common`.

Module-specific list models live in the owning module API or feature model package.

## 3. Features

Feature BFF endpoints may aggregate paginated responses but must not paginate large datasets in memory.

## 4. Catalog

Catalog exposes list APIs by default. Paging is allowed only for large/searchable catalogs and must be documented.

## 5. Core/platform

Core and platform list queries apply pagination at reader/persistence level, not in controllers.
