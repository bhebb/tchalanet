# Spec — Standardize document capability under platform.document

## Goal

Move document utilities that are application services to `platform.document` while leaving only generic primitives in `common`.

## New package

```text
com.tchalanet.server.platform.document.api.*
com.tchalanet.server.platform.document.internal.*
```

## Public API

```text
DocumentApi
GenerateDocumentRequest
GenerateDocumentResult
StoredDocumentView
DocumentType
```

## Internal implementation

```text
internal/app/DocumentService
internal/app/DocumentRenderer
internal/adapter/PdfAdapter or PdfBoxAdapter
internal/persistence/DocumentJpaEntity
internal/web/DocumentOpsController
```

## Keep in common

Only pure low-level helpers with no app workflow and no tenant/user persistence.

## Migration tasks

- [ ] Inventory document-related classes in common/core/features.
- [ ] Move generation/storage metadata workflow to platform.document.
- [ ] Keep ticket receipt formatting in core.sales if it is sales-specific.
- [ ] Expose only reusable document operations through DocumentApi.

## Verification

- [ ] No business-specific document rule moved out of owning core domain.
- [ ] No external import of platform.document.internal.
