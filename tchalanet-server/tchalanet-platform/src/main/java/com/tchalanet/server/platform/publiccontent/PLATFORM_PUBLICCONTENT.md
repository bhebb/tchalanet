# Platform Capability `platform.publiccontent` — Public Content Delivery

> Archetype : Application Service Module.

## 1. Rôle

Exposer et servir le contenu public (pages, assets, messages, CGU, etc.) destiné aux utilisateurs non authentifiés ou à large diffusion.

**Ce module fait** :
- Servir des pages ou assets publics (CGU, mentions légales, landing pages, etc.).
- Gérer la publication et la mise à jour de contenu public.
- Exposer une API de lecture pour le contenu public.

**Ce module ne fait pas** :
- Authentification ou gestion d’accès restreint (→ accesscontrol).
- Stockage de contenu métier privé ou sensible.
- Gestion de contenu multilingue avancée (à spécifier si besoin).

## 2. Structure

```text
platform/publiccontent/
  api/
    PublicContentApi.java         ← getContent(ContentKey)
    model/
      PublicContentView.java
      ContentKey.java
  internal/
    service/
    persistence/
    web/                         ← PublicContentController (/api/v1/public/content)
    config/
```

## 3. Règles

- Le contenu public doit être validé avant publication.
- Les modifications sont auditées via platform.audit.
- Pas de RLS (contenu non tenant-scoped, sauf exception à documenter).
- Les assets volumineux peuvent être stockés en externe (S3, CDN…).

## 4. Intégration

- Les apps web/mobiles consomment l’API pour afficher le contenu public.
- Les modifications de contenu sont traçables et auditées.

## 5. Guardrails

- Ne jamais exposer de données sensibles via ce module.
- Les dépendances doivent rester internes à platform/publiccontent.
