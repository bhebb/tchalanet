
# Platform Capability `platform.publiccontent` — Public Content Delivery

## Rôle

Exposer et servir le contenu public (pages, assets, messages, CGU, etc.) destiné aux utilisateurs non authentifiés ou à large diffusion.

**Ce module fait** :
- Service de pages/assets publics (CGU, mentions légales, landing, etc.)
- Publication/mise à jour de contenu public
- API de lecture pour le contenu public

**Ce module ne fait pas** :
- Authentification ou gestion d’accès restreint (voir accesscontrol)
- Stockage de contenu métier privé ou sensible
- Gestion multilingue avancée (à spécifier si besoin)

## Surface API

- `PublicContentApi` (Java) : `getContent(ContentKey)`
- Modèles : `PublicContentView`, `ContentKey`

## Intégration

- Les apps web/mobiles consomment l’API pour afficher le contenu public
- Les modifications sont auditées via platform.audit

## Règles et limitations

- Le contenu public doit être validé avant publication
- Pas de RLS (contenu non tenant-scoped, sauf exception)
- Les assets volumineux peuvent être stockés en externe (S3, CDN…)
- Ne jamais exposer de données sensibles via ce module
