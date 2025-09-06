
# Choix de la Stack Technique

## Statut
Accepté

## Contexte
Pour le développement du projet Tchalanet, nous avions besoin de choisir une stack technique qui réponde à nos besoins de développement multiplateforme (web et mobile), avec une architecture scalable et maintenable.

## Décision
Nous avons décidé d'adopter la stack technique suivante :

### Frontend
- **Web** : Angular avec Nx pour une architecture monorepo
- **Mobile** : Ionic basé sur Angular pour maximiser la réutilisation de code

### Backend
- **API** : Spring Boot pour ses performances et son écosystème robuste
- **Base de données** : PostgreSQL avec Row Level Security (RLS) pour une sécurité intégrée
- **Migrations** : Flyway pour la gestion des versions de schéma de base de données

### DevOps
- **CI/CD** : GitHub Actions pour l'automatisation des pipelines
- **Conteneurisation** : Docker et Docker Compose pour le développement local et la production
- **Infrastructure as Code** : Terraform pour provisionner l'infrastructure cloud

## Conséquences
- Réutilisation de code maximale entre les applications web et mobile
- Architecture monorepo qui facilite la gestion des dépendances et la cohérence du code
- Sécurité renforcée avec RLS directement au niveau de la base de données
- Maintenance simplifiée grâce aux migrations automatisées de la base de données
- Déploiement continu facilité par GitHub Actions
