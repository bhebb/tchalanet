# Tasks: infra-git-workflows-light

## 1. Inventaire

- [ ] Lister tous les workflows actuels.
- [ ] Identifier doublons `Docs CI`.
- [ ] Identifier workflows qui build/push sur `main`.
- [ ] Identifier workflows qui déploient prod.
- [ ] Identifier workflows qui incluent services post-v0.

## 2. Archivage

- [ ] Créer `.github/workflows/_archive/` ou documenter suppression dans `docs/legacy`.
- [ ] Archiver `Manage Keycloak Realm`.
- [ ] Archiver anciens déploiements production.
- [ ] Archiver workflow security scan auto.
- [ ] Supprimer un des deux `Docs CI`.

## 3. Créer workflows cibles

- [ ] Créer `server-pr.yml`.
- [ ] Créer `web-pr.yml`.
- [ ] Créer `edge-pr.yml`.
- [ ] Créer `infra-check.yml`.
- [ ] Créer `docs.yml` unique.
- [ ] Créer ou simplifier `deploy-staging.yml`.

## 4. Déploiement staging

- [ ] Ajouter input `build_api`.
- [ ] Ajouter input `build_web`.
- [ ] Ajouter input `build_edge`.
- [ ] Ajouter input `deploy_infra`.
- [ ] Ajouter input `image_tag`.
- [ ] Utiliser un `IMAGE_TAG` commun.
- [ ] Ne jamais publier `latest`.
- [ ] Déployer via Makefile côté serveur.

## 5. Validation

- [ ] Ouvrir PR backend et vérifier que seul `server-pr.yml` tourne.
- [ ] Ouvrir PR web et vérifier que seul `web-pr.yml` tourne.
- [ ] Ouvrir PR edge et vérifier que seul `edge-pr.yml` tourne.
- [ ] Ouvrir PR infra et vérifier que seul `infra-check.yml` tourne.
- [ ] Vérifier que push main ne build pas Docker automatiquement.

## 6. Documentation

- [ ] Documenter les workflows dans `tchalanet-infra/docs/` ou `tchalanet-docs`.
- [ ] Documenter que staging est manuel.
- [ ] Documenter que prod est hors scope v0.
