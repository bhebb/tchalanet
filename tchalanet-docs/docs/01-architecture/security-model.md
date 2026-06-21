# Security model

## Scopes API (routing)

- PUBLIC: `/api/v1/public/**`
- TENANT: `/api/v1/tenant/**`
- ADMIN: `/api/v1/admin/**`
- PLATFORM: `/api/v1/platform/**`
- SDR: `/_sdr/**`

## AuthN / AuthZ

- **AuthN** : Firebase JWT — tous les acteurs (APP_USER et SELLER_TERMINAL)
- **AuthZ** : permissions centralisées (pas de logique ad-hoc dans chaque controller)

### Deux types d'acteurs

| `TchActorType` | Source de l'identité | Authority Spring |
|---|---|---|
| `APP_USER` | `ExternalAuthenticatedUser(provider, issuer, subject)` → `app_user_external_identity` | `ACTOR_APP_USER` |
| `SELLER_TERMINAL` | `ExternalAuthenticatedUser(provider, issuer, subject)` → `seller_terminal_external_identity` | `ACTOR_SELLER_TERMINAL` |

> **Provider actif V0 : Firebase.** Le subject est le Firebase UID. Tchalanet reste provider-neutral : aucune colonne `firebase_uid` dans les tables métier — le subject vit dans `*_external_identity`.  
> **Pas de Keycloak.** L'ancien flow Keycloak JWT a été remplacé par Firebase pour tous les acteurs.

### Pipeline d'authentification

```
Client (web / mobile / POS)
  ↓ Authorization: Bearer <provider-id-token>
Spring Security Resource Server
  ↓ JwtDecoder (Firebase public keys en V0)
IdentityProviderApi.mapVerifiedToken()
  ↓ ExternalAuthenticatedUser(provider, issuer, subject)
  ↓ résout APP_USER ou SELLER_TERMINAL via *_external_identity
TchAccessContextPipelineFilter
  ↓ charge roles, permissions, sellerTerminalId
TchContextFilter
  ↓ construit TchRequestContext (actorType, sellerTerminalId, permissionKeys…)
Controller / CommandBus / QueryBus
  ↓ business flow
RlsAwareDataSource → PostgreSQL RLS
```

## Multi-tenant isolation

- Tenant résolu via request context (jamais depuis le client payload)
- PostgreSQL RLS comme dernière ligne de défense
- Aucun `tenant_id` venant du client n'est fiable

## Contrôle d'accès SellerTerminal

Les SellerTerminals n'ont **pas** de rôles Tchalanet. Leur accès est contrôlé par :
- la présence de l'authority `ACTOR_SELLER_TERMINAL`
- le statut du terminal (`ACTIVE` / `BLOCKED` / `DISABLED`)
- le flag `mustChangePin` (bloque les actions de vente jusqu'au changement de PIN)
- les permissions directement attachées à l'entité (ex: `ticket.sell`)

Voir : `tchalanet-server/docs/conventions/context/role-flows.md`

## Public endpoints (règles)

- explicites
- rate-limited
- validation stricte
- noindex quand nécessaire (`/ticket/:code`)

## Liens

- Authentication flow : `tchalanet-docs/docs/01-architecture/flows/authentication-flow.md`
- Identity providers : `tchalanet-server/docs/conventions/identity-providers.md`
- Role flows : `tchalanet-server/docs/conventions/context/role-flows.md`
- Backend non-negotiables : `tchalanet-server/openspec/context/10-non-negotiables.md`
