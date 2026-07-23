# RB-05 — Déployer la documentation publique sur Cloudflare Pages

## Objectif

Publier la documentation testeurs Tchalanet sur `docs.tchalanet.com` avec un
déploiement manuel depuis GitHub Actions.

La surface publique doit rester séparée de la documentation technique. Le site
publié est construit uniquement depuis `tchalanet-docs/docs-public/` avec
`tchalanet-docs/mkdocs.public.yml`.

Le modèle suit la même philosophie que le web Tchalanet : générer un dossier
statique prêt pour Cloudflare Pages, inclure les fichiers Cloudflare dans
l'artefact, puis publier ce dossier avec Wrangler.

## TODO configuration initiale

- [ ] Créer un projet Cloudflare Pages nommé `tchalanet-docs-public`.
- [ ] Configurer le domaine personnalisé `docs.tchalanet.com` dans Cloudflare
      Pages.
- [ ] Vérifier que le DNS `docs.tchalanet.com` pointe vers Cloudflare Pages.
- [ ] Vérifier que le certificat TLS Cloudflare est actif.
- [ ] Créer un token Cloudflare API limité au déploiement Pages.
- [ ] Ajouter les valeurs GitHub Actions :
  - secret `CLOUDFLARE_API_TOKEN` ;
  - secret ou variable `CLOUDFLARE_ACCOUNT_ID` ;
  - variable `CLOUDFLARE_PAGES_PROJECT_DOCS_PUBLIC=tchalanet-docs-public`.
- [ ] Lancer le workflow manuel `Docs CI` avec :
  - `docs_surface=public` ;
  - `deploy_public=true`.
- [ ] Valider `https://docs.tchalanet.com` après le premier déploiement.

## Permissions Cloudflare minimales

Le token API doit permettre de publier sur Cloudflare Pages pour le compte qui
contient `tchalanet.com`.

Permissions recommandées :

- Account / Cloudflare Pages / Edit.
- Account / Account Settings / Read.

Le DNS peut être configuré manuellement dans le dashboard Cloudflare. Si on veut
automatiser aussi la création du domaine ou des records DNS, ajouter seulement
à ce moment :

- Zone / DNS / Edit.
- Zone / Zone / Read.

## Configuration Cloudflare Pages

Valeurs cibles :

| Champ | Valeur |
|---|---|
| Project name | `tchalanet-docs-public` |
| Production branch | `main` |
| Build command | géré par GitHub Actions |
| Output directory | `tchalanet-docs/site-public` |
| Custom domain | `docs.tchalanet.com` |

Le projet Cloudflare Pages peut rester sans build automatique si GitHub Actions
fait le déploiement via Wrangler.

Le build public doit contenir :

- `_headers` pour cache et en-têtes de sécurité ;
- `_redirects` pour les routes courtes ;
- uniquement le contenu généré depuis `docs-public/`.

## Déploiement manuel

Dans GitHub :

1. Ouvrir l'onglet Actions.
2. Choisir `Docs CI`.
3. Cliquer sur `Run workflow`.
4. Choisir la branche à publier.
5. Mettre `docs_surface=public`.
6. Mettre `deploy_public=true`.
7. Lancer le workflow.

Pour publier le domaine principal `docs.tchalanet.com`, lancer le workflow depuis
`main`. Depuis une branche de PR, Cloudflare Pages crée plutôt un déploiement de
prévisualisation.

## Validation après déploiement

- [ ] Ouvrir `https://docs.tchalanet.com`.
- [ ] Vérifier que la recherche ne retourne pas de contenu technique.
- [ ] Vérifier les pages :
  - `/admin/` ;
  - `/owner/` ;
  - `/super-admin/` ;
  - `/pos/` ;
  - `/validation/scenarios-de-test/` ;
  - `/validation/signaler-un-probleme/`.
- [ ] Vérifier le rendu mobile.
- [ ] Vérifier que les liens vers admin, mobile/POS et signalement pointent vers
      les bonnes cibles de staging.
- [ ] Vérifier que le site ne mentionne pas de secrets, chemins internes,
      détails d'architecture ou procédures d'exploitation internes.

## Rollback

Option recommandée :

1. Ouvrir Cloudflare Dashboard.
2. Aller dans Pages.
3. Ouvrir `tchalanet-docs-public`.
4. Aller dans Deployments.
5. Sélectionner le dernier déploiement stable.
6. Cliquer sur Rollback.

Option GitHub :

1. Revenir au SHA ou tag de documentation stable.
2. Relancer `Docs CI` en manuel depuis cette révision.

## Surface technique

Ne pas publier la documentation technique pour la livraison week-end.

Quand elle sera nécessaire :

- créer un projet séparé, par exemple `tchalanet-docs-internal` ;
- utiliser un domaine séparé, par exemple `docs-internal.tchalanet.com` ;
- protéger l'accès avec Cloudflare Access ou un réseau privé ;
- construire depuis `tchalanet-docs/mkdocs.internal.yml`.
