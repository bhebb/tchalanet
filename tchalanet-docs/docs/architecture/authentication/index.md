## Authentification
### OAuth 2.0 / OpenID Connect
L'authentification est gérée via OAuth 2.0 et OpenID Connect, avec support pour :
- Authentification par mot de passe
- Authentification via fournisseurs externes (Google, GitHub, etc.)
- Single Sign-On (SSO)

### Flux d'authentification
1. L'utilisateur s'authentifie auprès du service d'authentification
2. Le service génère un JWT (JSON Web Token) signé
3. Le client stocke le JWT et l'envoie dans l'en-tête `Authorization` pour les requêtes API
4. Le serveur valide le JWT avant de traiter la requête

## Autorisation
### Contrôle d'accès basé sur les rôles (RBAC)
L'autorisation est gérée via un système de rôles et permissions :
