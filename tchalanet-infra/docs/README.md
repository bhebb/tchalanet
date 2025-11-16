# 📚 Documentation Tchalanet Infrastructure

Index des guides et documentation pour l'infrastructure Tchalanet.

## 🚀 Démarrage Rapide

- **[QUICKSTART.md](QUICKSTART.md)** - Démarrage rapide local (5 min)
- **[QUICKSTART-IMAGES.md](QUICKSTART-IMAGES.md)** ⭐ **NOUVEAU** - Publication rapide des images Docker (5 min)

## 🐳 Images Docker & Déploiement

- **[IMAGES-DEPLOYMENT.md](IMAGES-DEPLOYMENT.md)** - Guide complet publication/déploiement des images
- **[ENV-SEPARATION.md](ENV-SEPARATION.md)** - Séparation build-time / runtime variables

## 🔐 Secrets & Configuration

- **[DOPPLER.md](DOPPLER.md)** - Vue d'ensemble Doppler
- **[DOPPLER-SETUP-GUIDE.md](DOPPLER-SETUP-GUIDE.md)** - Setup Doppler complet
- **[DOPPLER-DOWNLOAD-SECRETS.md](DOPPLER-DOWNLOAD-SECRETS.md)** - Téléchargement des secrets

## 🏗️ Infrastructure & Hébergement

- **[HETZNER.md](HETZNER.md)** - Configuration serveur Hetzner (création, réseau, firewall)
- **[TRAEFIK-STAGING-SETUP.md](TRAEFIK-STAGING-SETUP.md)** - Configuration Traefik pour staging
- **[infrastructure.md](infrastructure.md)** - Architecture infrastructure

## 🚢 Déploiement

- **[DEPLOYMENT.md](DEPLOYMENT.md)** ⭐ **NOUVEAU** - Guide complet de déploiement staging/production
- **[DEMARRAGE.md](DEMARRAGE.md)** - Démarrage des services
- **[OPERATIONS.md](OPERATIONS.md)** - Opérations courantes

## 🐛 Corrections & Troubleshooting

- **[SUMMARY-ENV-OPTIMIZATION.md](SUMMARY-ENV-OPTIMIZATION.md)** ⭐ **NOUVEAU** - Résumé optimisation architecture variables
- **[FIX-DOCKER-COMPOSE-INTERPOLATION.md](FIX-DOCKER-COMPOSE-INTERPOLATION.md)** ⭐ **NOUVEAU** - Correction variables d'interpolation Docker Compose (14 nov 2025)

## 🔒 Sécurité

- **[SECURITY.md](SECURITY.md)** - Pratiques de sécurité
- **[SECURITY_PLAN.md](SECURITY_PLAN.md)** - Plan de sécurité détaillé

## 🛠️ Scripts

- **[scripts-index.md](scripts-index.md)** - Index de tous les scripts disponibles
- **[../scripts/README.md](../scripts/README.md)** - Organisation des scripts

## 📖 Références

- **[api.env.example](api.env.example)** - Exemple de configuration API
- **[../compose/docker-compose.index.md](../compose/docker-compose.index.md)** - Index des fichiers compose

---

## 🎯 Par Cas d'Usage

### Je veux démarrer en local (dev)

👉 [QUICKSTART.md](QUICKSTART.md)

### Je veux publier de nouvelles images

👉 [QUICKSTART-IMAGES.md](QUICKSTART-IMAGES.md) puis [IMAGES-DEPLOYMENT.md](IMAGES-DEPLOYMENT.md)

### Je veux créer un nouveau serveur

👉 [HETZNER.md](HETZNER.md) puis [DEPLOYMENT.md](DEPLOYMENT.md)

### Je veux configurer les secrets

👉 [DOPPLER-SETUP-GUIDE.md](DOPPLER-SETUP-GUIDE.md)

### Je veux déployer sur staging/prod

👉 [DEPLOYMENT.md](DEPLOYMENT.md) section "Déploiement"

### Je veux comprendre l'architecture

👉 [infrastructure.md](infrastructure.md)

---

## 🆕 Mises à Jour Récentes

**14 novembre 2025**

- ✅ Ajout guide publication images Docker (QUICKSTART-IMAGES.md)
- ✅ Correction script `publish-images.sh` (auth + chemins)
- ✅ Documentation complète déploiement images (IMAGES-DEPLOYMENT.md)
- ✅ Workflow GitHub Actions pour publication automatique

**5 novembre 2025**

- Setup Doppler pour gestion des secrets
- Documentation Hetzner (création serveur, réseau, firewall)
- Amélioration scripts de déploiement

---

## 🆘 Besoin d'Aide ?

1. Consulter le guide approprié ci-dessus
2. Vérifier les troubleshooting dans chaque guide
3. Consulter les scripts : `tchalanet-infra/scripts/*/README.md`
4. Vérifier les logs : `docker compose logs -f <service>`

## 🤝 Contribuer

Pour améliorer la documentation :

1. Identifier le document concerné
2. Proposer les modifications
3. Mettre à jour l'index (ce fichier) si nécessaire
4. Mettre à jour la date "Dernière mise à jour" dans le document
