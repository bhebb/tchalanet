# Tasks — Portail docs public/testeurs et documentation technique

## T0 — Spec

- [x] Créer `proposal.md`
- [x] Créer `tasks.md`
- [x] Créer les deltas de spec

## T1 — Inventaire et stratégie de séparation

- [ ] Auditer `tchalanet-docs/mkdocs.yml` pour classifier chaque page :
  - public/testeurs ;
  - métier semi-public ;
  - technique interne ;
  - référence interne.
- [ ] Créer une table d'inventaire avec :
  chemin, audience, propriétaire, statut et build cible.
- [x] Choisir une séparation physique sûre :
  `docs-public/` pour le public et `docs/` pour l'interne.
- [x] Créer `mkdocs.public.yml`.
- [x] Créer ou adapter `mkdocs.internal.yml`.
- [x] Configurer le build public avec `docs_dir: docs-public`.
- [ ] Vérifier que le build public ne copie aucun fichier technique non référencé.
- [ ] Vérifier que navigation, search index et sitemap ne contiennent que les
  pages publiques autorisées.

## T2 — Structure du portail public

- [x] Créer l'accueil testeurs.
- [x] Afficher clairement :
  environnement, version, date, périmètre et avertissement données de test.
- [x] Expliquer les rôles :
  Admin, Owner, Super Admin et Vendeur/POS.
- [ ] Ajouter les liens vers :
  application admin, application mobile/POS et formulaire de signalement.
- [x] Ajouter une navigation courte orientée tâches, pas architecture.

## T3 — Guides par rôle

- [x] Créer le guide Admin :
  connexion, configuration courante, jeux, canaux, résultats, tickets et rapports.
- [x] Créer le guide Owner :
  entreprise/santral, vendeurs, terminaux, commissions, limites et suivi.
- [x] Créer le guide Super Admin :
  tenant, provider, statuts, support et overrides autorisés.
- [x] Créer le guide Vendeur / Terminal POS :
  connexion, changement PIN, vente, confirmation, impression, consultation et
  réimpression.
- [x] Documenter pour chaque rôle :
  actions permises, actions interdites et erreurs courantes.

## T4 — Validation testeurs

- [x] Créer la page `Scénarios de test`.
- [x] Utiliser le format :
  ID, rôle, préconditions, étapes, résultat attendu, évidence.
- [ ] Couvrir au minimum :
  - connexion admin ;
  - création/configuration terminal ;
  - connexion POS ;
  - changement de PIN ;
  - vente simple ;
  - annulation avant confirmation ;
  - impression ;
  - réimpression ;
  - consultation ticket ;
  - saisie/consultation résultat ;
  - rapport de base.
- [x] Créer la page `Signaler un problème`.
- [x] Demander :
  environnement, rôle, scénario, étapes, attendu, obtenu, capture, heure,
  code ticket et trace/error ID lorsque disponible.
- [x] Ajouter une FAQ courte :
  accès, PIN, terminal bloqué, tirage fermé, impression et staging/prod.

## T5 — Documentation technique/interne

- [ ] Garder Architecture, Technique, OpenSpec, Référence interne hors du build
  public.
- [ ] Documenter que `docs/ARCHITECTURE.md` reste la source structurelle normative.
- [ ] Documenter que le `PLAYBOOK` et les conventions restent des références
  d'implémentation internes.
- [ ] Définir la stratégie interne :
  non déployée, réseau privé ou Cloudflare Access.
- [ ] Ne pas exposer la surface interne pour le week-end sauf besoin confirmé.

## T6 — Déploiement Cloudflare Workers

- [x] Préparer le workflow manuel de déploiement public.
- [x] Builder uniquement avec `mkdocs.public.yml`.
- [x] Publier uniquement le dossier `site-public/`.
- [x] Ajouter la configuration Wrangler du Worker static assets.
- [ ] Créer ou identifier le Worker Cloudflare public.
- [ ] Configurer le domaine `docs.tchalanet.com`.
- [ ] Ajouter les secrets/variables GitHub Actions :
  `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`.
- [ ] Lancer un premier déploiement manuel depuis `main`.
- [ ] Vérifier l'URL de production.

## T7 — Déploiement

- [x] Ajouter le runbook Cloudflare Workers.
- [ ] Préparer le DNS Cloudflare pour `docs.tchalanet.com`.
- [ ] Valider TLS et redirection HTTP vers HTTPS.
- [ ] Documenter le rollback :
  rollback Worker ou redéploiement d'un SHA stable.
- [ ] Ne placer aucun secret dans le contenu publié ou le repository docs.

## T8 — Validation CI/CD

- [x] Exécuter `mkdocs build -f mkdocs.public.yml --strict`.
- [ ] Exécuter `mkdocs build -f mkdocs.internal.yml --strict`.
- [ ] Vérifier les liens internes du site public.
- [x] Préparer le déploiement public manuel dans GitHub Actions.
- [ ] Ajouter un test anti-fuite sur :
  HTML, sitemap et search index.
- [x] Exécuter un scan anti-fuite manuel initial sur :
  HTML, sitemap et search index.
- [ ] Faire échouer la CI si une page hors allowlist apparaît dans le build public.
- [ ] Faire échouer la CI si des termes ou chemins internes apparaissent dans le
  build public, par exemple :
  `ARCHITECTURE`, `OpenSpec`, `ADR`, `Flyway`, `Redis`, `RLS`,
  `Firebase UID`, `Cloudflare Access`, `runbook`, `internal/`.
- [ ] Vérifier manuellement `docs.tchalanet.com` après déploiement.
- [ ] Tester le rendu mobile.

## T9 — Après week-end

- [ ] Évaluer une surface interne protégée pour la doc technique.
- [ ] Ajouter le versioning par release.
- [ ] Ajouter les captures mobile/admin validées.
- [ ] Ajouter progressivement anglais et créole haïtien.
- [ ] Définir un propriétaire et une date de revue pour chaque guide.

## Priorité réaliste week-end

- [ ] Séparation physique `docs-public/`.
- [ ] Homepage testeurs.
- [ ] Guide Admin.
- [ ] Guide POS.
- [ ] Scénarios de validation.
- [ ] Signalement de problème.
- [ ] Build strict.
- [ ] Déploiement manuel Cloudflare Workers.
