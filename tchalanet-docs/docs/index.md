# Documentation Tchalanet

Bienvenue dans la documentation technique du projet Tchalanet. Cette documentation couvre tous les aspects techniques du projet, y compris l'architecture, les guides de développement et les procédures opérationnelles.

## Composants principaux

- **Frontend Web** : Application Angular gérée avec Nx dans une architecture monorepo
- **Frontend Mobile** : Application Ionic Angular pour iOS et Android
- **Backend** : API REST développée avec Spring Boot
- **Infrastructure** : PostgreSQL, Redis, et solutions cloud pour le déploiement

## Pour commencer

- Pour en savoir plus sur l'architecture globale, consultez la [Vue d'ensemble](architecture/overview.md)
- Pour configurer l'environnement de développement backend, consultez le [Guide de configuration](backend/setup.md)
- Pour comprendre l'architecture web, voir [Architecture Web](web/architecture.md)

## Décisions d'architecture

Les décisions d'architecture importantes sont documentées dans les [ADR (Architecture Decision Records)](adr/0001-stack.md) pour maintenir une trace des choix techniques et de leurs justifications.

## Contribuer à la documentation

Cette documentation est gérée avec MkDocs. Pour contribuer :

1. Clonez le dépôt `tchalanet-docs`
2. Installez MkDocs : `pip install mkdocs mkdocs-material`
3. Lancez le serveur local : `mkdocs serve`
4. Accédez à la documentation sur http://127.0.0.1:8000
5. Modifiez les fichiers Markdown dans le dossier `docs/`
