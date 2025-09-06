# Vue d'ensemble

``` mermaid
graph LR
B %% Clients WEB[Web Angular Nx] MOBILE[Mobile Ionic Angular]
%% Backend
API[API Spring Boot]
AUTH[Auth Service]
NOTIF[Service de Notifications]
MEDIA[Service de Média]

%% Infrastructure
DB[(PostgreSQL + RLS)]
CACHE[(Redis Cache)]
STORAGE[Object Storage]
QUEUE[Message Queue]

%% Connexions
WEB -->|OIDC + REST| API
MOBILE -->|OIDC + REST| API

API --> DB
API --> CACHE
API --> STORAGE

API <--> AUTH
API <--> NOTIF
API <--> MEDIA

NOTIF --> QUEUE
MEDIA --> STORAGE
```


## Description des composants

### Frontend
- **Web (Angular Nx)** : Application web développée avec Angular et utilisant Nx pour la gestion du monorepo
- **Mobile (Ionic Angular)** : Application mobile cross-platform développée avec Ionic et Angular

### Backend
- **API Spring Boot** : Service principal exposant des APIs REST pour les applications clientes
- **Service d'Authentification** : Gestion des identités et des accès via OIDC
- **Service de Notifications** : Gestion des notifications push et emails

### Infrastructure
- **PostgreSQL** : Base de données relationnelle avec Row Level Security (RLS) et migrations Flyway
- **Redis Cache** : Cache distribué pour améliorer les performances
- **Object Storage** : Stockage des fichiers et médias (images, documents, etc.)

## Flux de données principaux

1. Les clients (Web et Mobile) s'authentifient auprès du service d'authentification via OIDC
2. Une fois authentifiés, ils peuvent accéder aux ressources de l'API REST
3. L'API interagit avec la base de données, le cache et le stockage d'objets selon les besoins
4. Les notifications sont envoyées aux utilisateurs via le service de notifications

## Considérations techniques

- **Sécurité** : Authentification OIDC, tokens JWT, HTTPS, RLS au niveau base de données
- **Performance** : Mise en cache avec Redis, optimisation des requêtes
- **Scalabilité** : Architecture modulaire permettant le scaling horizontal
- **Maintenance** : CI/CD automatisé, tests exhaustifs, monitoring
