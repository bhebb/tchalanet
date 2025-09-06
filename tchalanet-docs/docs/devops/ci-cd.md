# CI/CD
Cette page détaille le processus d'intégration continue et de déploiement continu mis en place pour Tchalanet.
## Aperçu du processus CI/CD
graph LR
COMMIT[Commit/PR] --> BUILD[Build]
BUILD --> TEST[Tests]
TEST --> QUALITY[Analyse de Qualité]
QUALITY --> ARTIFACT[Création d'Artifacts]
ARTIFACT --> DEPLOY_DEV[Déploiement DEV]
DEPLOY_DEV -->|Si branche main| DEPLOY_STAGING[Déploiement STAGING]
DEPLOY_STAGING -->|Release manuelle| DEPLOY_PROD[Déploiement PROD]

## Infrastructure GitHub Actions
Tchalanet utilise GitHub Actions pour automatiser le CI/CD. Les workflows sont définis dans le dossier et comprennent : `.github/workflows/`
### Workflow de CI pour le frontend

## Environnements
### Dev
Environnement de développement pour tester les fonctionnalités en cours de développement.
- **URL**: [https://dev.tchalanet.com](https://dev.tchalanet.com)
- **Déclenchement**: Automatique sur les commits sur `develop`
- **Infrastructure**: AWS (S3, ECS, RDS)

### Staging
Environnement de pré-production pour valider les fonctionnalités avant la mise en production.
- **URL**: [https://staging.tchalanet.com](https://staging.tchalanet.com)
- **Déclenchement**: Automatique sur les commits sur `main`
- **Infrastructure**: AWS (S3, ECS, RDS)

### Production
Environnement de production pour les utilisateurs finaux.
- **URL**: [https://tchalanet.com](https://tchalanet.com)
- **Déclenchement**: Manuel après validation sur staging
- **Infrastructure**: AWS (CloudFront, S3, ECS, RDS)

## Infrastructure as Code
L'infrastructure est gérée avec Terraform pour garantir la reproductibilité et la cohérence entre les environnements.

## Qualité et sécurité
### Analyse de qualité de code
- **SonarQube** : Analyse statique du code pour détecter les bugs, vulnérabilités et problèmes de maintainabilité.
- **ESLint** : Linting du code JavaScript/TypeScript.
- **Checkstyle** : Vérification du style du code Java.
- **SpotBugs** : Détection des bugs potentiels dans le code Java.

### Analyse de dépendances
### 
- : Vérification des mises à jour des dépendances npm. **npm-check-updates**
- **OWASP Dependency-Check** : Détection des vulnérabilités dans les dépendances.

### Politique de sécurité
- **Scans de sécurité** réguliers sur l'infrastructure.
- **Rotation des secrets** via AWS Secrets Manager.
- **WAF** (Web Application Firewall) pour protéger contre les attaques courantes.
- **VPC** isolé pour les ressources de backend.

## Monitoring et alerting
### Supervision de l'infrastructure
- **CloudWatch** pour la surveillance des métriques d'AWS.
- **Grafana + Prometheus** pour les tableaux de bord de visualisation.
- **Elasticsearch + Kibana** pour l'analyse des logs.

### Alerting
- **CloudWatch Alarms** pour les alertes sur les métriques.
- **PagerDuty** pour la gestion des astreintes.
- **Slack** pour les notifications d'équipe.
- 
## Stratégie de déploiement
### Blue/Green Deployment
Pour les déploiements sans interruption de service :
1. Une nouvelle version est déployée en parallèle de la version actuelle (environnement vert)
2. Tests de validation sur le nouvel environnement
3. Bascule du trafic de l'environnement actuel (bleu) vers le nouvel environnement (vert)
4. L'ancien environnement est conservé pour un rollback rapide si nécessaire

### Rollbacks
Procédure en cas d'incident suite à un déploiement :
1. Bascule immédiate vers l'environnement précédent
2. Analyse post-mortem de l'incident
3. Correction du bug et tests approfondis avant nouveau déploiement

