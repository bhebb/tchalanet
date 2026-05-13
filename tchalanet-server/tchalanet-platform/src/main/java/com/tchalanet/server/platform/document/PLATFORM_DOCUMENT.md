# Platform Capability `platform.document` — Document Generation & Storage

> Archetype : Application Service Module.

## 1. Rôle

Générer des documents (PDF, reçus, rapports) et les stocker de façon accessible.

**Ce module fait** :
- Générer un document depuis un template et des données (`DocumentApi.generate(DocumentRequest)`).
- Stocker les documents générés (S3, filesystem, DB blob).
- Exposer l'URL ou le contenu d'un document par ID.

**Ce module ne fait pas** :
- Calcul du contenu métier (le caller fournit les données).
- Livraison email (→ `platform.communication`).

## 2. Structure

```text
platform/document/
  api/
    DocumentApi.java          ← generate(DocumentRequest) → DocumentRef
    model/
      DocumentRequest.java    ← templateKey, data, format
      DocumentRef.java        ← documentId, url, mimeType
  internal/
    service/
    adapter/                  ← PdfGenerator, StorageAdapter
    persistence/              ← DocumentMetaJpaEntity
    web/                      ← DocumentController (/api/v1/tenant/documents/{id})
    config/
```

## 3. Règles

- RLS actif sur les métadonnées de documents.
- Format de sortie déclaré dans la requête (PDF, CSV, etc.).
- Génération synchrone ou asynchrone selon la taille — documenter le contrat dans l'API.
