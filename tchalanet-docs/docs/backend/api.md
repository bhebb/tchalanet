# API REST
Cette page documente l'API REST du backend Tchalanet, sa structure et les bonnes pratiques à suivre.
## Principes de conception
L'API Tchalanet suit les principes REST et adopte les conventions suivantes :
- Utilisation des méthodes HTTP standards (GET, POST, PUT, DELETE)
- Représentation des ressources en JSON
- Gestion des erreurs avec les codes HTTP appropriés
- Versionnement via le préfixe d'URL (`/api/v1/...`)
- Pagination des résultats pour les collections volumineuses
- Support du filtrage, tri et recherche via les paramètres de requête

## Structure de l'API
graph TD
ROOT[/api/v1] --> USERS[/users]
ROOT --> AUTH[/auth]
ROOT --> POSTS[/posts]
ROOT --> MESSAGES[/messages]
ROOT --> SEARCH[/search]

    USERS --> USER_ID[/{id}]
    USER_ID --> FOLLOW[/follow]
    USER_ID --> FOLLOWERS[/followers]
    USER_ID --> FOLLOWING[/following]
    USER_ID --> PROFILE[/profile]
    
    POSTS --> POST_ID[/{id}]
    POST_ID --> COMMENTS[/comments]
    POST_ID --> LIKES[/likes]

## Documentation OpenAPI
L'API est documentée avec OpenAPI 3.0 (Swagger). La documentation est générée automatiquement à partir des annotations dans le code.

La documentation Swagger UI est accessible à l'adresse :
http://localhost:8080/swagger-ui/index.html

## Gestion des erreurs
Les erreurs sont retournées au format JSON avec un code HTTP approprié :

### Codes d'erreur courants

| Code HTTP | Description |
| --- | --- |
| 400 | Bad Request - La requête contient des erreurs |
| 401 | Unauthorized - Authentification nécessaire |
| 403 | Forbidden - Accès interdit |
| 404 | Not Found - Ressource non trouvée |
| 409 | Conflict - Conflit avec l'état actuel de la ressource |
| 422 | Unprocessable Entity - Données invalides |
| 429 | Too Many Requests - Limite de taux dépassée |
| 500 | Internal Server Error - Erreur serveur |
