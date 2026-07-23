# docs-portal-public-private Specification

## Purpose

Définir comment Tchalanet publie une documentation MkDocs lisible pour les
testeurs tout en conservant une documentation technique séparée et non exposée
par accident.

## ADDED Requirements

### Requirement: Public tester docs SHALL be audience-scoped

Le portail public/testeurs SHALL présenter uniquement du contenu utile à la
validation fonctionnelle et opérationnelle de la livraison mobile/admin.

#### Scenario: Tester opens public docs

- **WHEN** un testeur ouvre `docs.tchalanet.com`
- **THEN** il voit une homepage orientée livraison et validation
- **AND** il peut choisir un parcours par rôle
- **AND** il ne voit pas de sections architecture, conventions internes,
  OpenSpec, matrices techniques ou références AI.

#### Scenario: Tester sees environment banner

- **WHEN** un testeur ouvre une page publique
- **THEN** la page indique l'environnement `STAGING`
- **AND** elle indique que les données sont des tests uniquement
- **AND** elle indique qu'aucun ticket ou montant n'a de valeur réelle
- **AND** elle affiche la version testée et la date de dernière mise à jour.

#### Scenario: Public docs search

- **WHEN** un testeur utilise la recherche MkDocs publique
- **THEN** les résultats SHALL rester dans le corpus public/testeurs
- **AND** aucun contenu technique interne SHALL apparaître dans l'index de
  recherche public.

### Requirement: Role-based validation guides SHALL exist

Le build public/testeurs SHALL contenir des guides orientés rôles pour les
validations de livraison.

#### Scenario: Role definitions are distinct

- **WHEN** un testeur consulte l'accueil public
- **THEN** le rôle Owner est défini comme supervision entreprise/santral,
  vendeurs, terminaux, limites, commissions et activité
- **AND** le rôle Admin est défini comme configuration et opérations
  quotidiennes autorisées
- **AND** le rôle Super Admin est défini comme support plateforme et opérations
  cross-tenant
- **AND** le rôle Vendeur/POS est défini comme ventes, tickets, impression et
  consultation.

#### Scenario: Admin tester validates admin portal

- **WHEN** un testeur admin consulte la documentation
- **THEN** il trouve un cheminement pour connexion, configuration générale,
  jeux, canaux de tirage, résultats, tickets et rapports de base.

#### Scenario: Owner validates tenant operations

- **WHEN** un owner/propriétaire de santral consulte la documentation
- **THEN** il trouve un cheminement pour informations entreprise, terminaux POS,
  utilisateurs/rôles, limites et suivi opérationnel.

#### Scenario: Super admin validates support operations

- **WHEN** un super admin consulte la documentation
- **THEN** il trouve un cheminement pour support tenant, activation provider,
  contrôle/override des résultats, et validation des statuts.

#### Scenario: Seller validates POS mobile

- **WHEN** un vendeur ou testeur POS consulte la documentation
- **THEN** il trouve un cheminement pour connexion terminal, vente, impression ou
  reçu, consultation de ticket et actions permises.

### Requirement: Technical docs SHALL be separable from public docs

La documentation technique SHALL pouvoir être construite, déployée ou retenue
indépendamment du site public/testeurs.

#### Scenario: Public build uses physical docs directory

- **WHEN** le site public est construit
- **THEN** MkDocs utilise `mkdocs.public.yml`
- **AND** la configuration publique utilise `docs_dir: docs-public`
- **AND** le build public ne lit pas `docs/` comme source documentaire.

#### Scenario: Internal build uses internal docs directory

- **WHEN** le site technique est construit
- **THEN** MkDocs utilise `mkdocs.internal.yml`
- **AND** la configuration interne utilise `docs_dir: docs`
- **AND** `docs/ARCHITECTURE.md`, les `PLAYBOOK`, conventions, ADR, OpenSpec et
  runbooks restent dans le corpus interne.

#### Scenario: Public site is deployed

- **WHEN** le site public est déployé
- **THEN** les pages techniques ne sont pas incluses dans l'artefact publié
- **AND** les fichiers techniques ne sont pas accessibles par URL directe dans
  le site public.

#### Scenario: Internal docs are deployed

- **WHEN** la documentation technique est déployée
- **THEN** elle SHALL être sous un sous-domaine ou chemin distinct
- **AND** elle SHALL être protégée par Cloudflare Access, VPN, ou un mécanisme
  équivalent avant d'être exposée hors réseau privé.

### Requirement: Docs site SHALL be static-host deployable

Le site public/testeurs SHALL être généré en statique par MkDocs et publié sur
un hébergement statique sans secret applicatif dans l'artefact.

#### Scenario: Build public docs artifact

- **WHEN** l'artefact docs public est construit
- **THEN** elle exécute un build MkDocs public
- **AND** elle publie uniquement le dossier statique généré
- **AND** aucun secret, fichier `.env`, clé ou token n'est inclus dans
  l'artefact.

#### Scenario: Public artifact excludes source corpus

- **WHEN** l'artefact public est inspecté
- **THEN** elle contient le site statique généré
- **AND** elle ne contient pas `.git`
- **AND** elle ne contient pas `docs/`
- **AND** elle ne contient pas `mkdocs.internal.yml`
- **AND** elle ne contient pas de sources techniques internes.

#### Scenario: Cloudflare Pages alias is configured

- **WHEN** `docs.tchalanet.com` est activé dans Cloudflare Pages
- **THEN** il route vers le projet docs public
- **AND** TLS est actif
- **AND** la racine affiche le portail testeurs.

### Requirement: Weekend delivery SHALL prioritize minimal usable documentation

La livraison weekend SHALL privilégier un corpus court et validable plutôt
qu'une migration exhaustive de toute la documentation.

#### Scenario: Weekend docs are not complete

- **WHEN** une page technique complète n'est pas prête
- **THEN** elle reste hors du build public
- **AND** une page publique courte peut pointer vers les actions de validation
  nécessaires sans exposer les détails internes.

#### Scenario: Tester reports an issue

- **WHEN** un testeur rencontre un problème
- **THEN** la documentation publique lui donne un format de remontée incluant
  rôle, environnement, action effectuée, résultat attendu, résultat obtenu,
  capture/log si disponible.

#### Scenario: Tester executes validation scenario

- **WHEN** un testeur suit un scénario de validation
- **THEN** le scénario indique un ID stable
- **AND** il indique le rôle concerné
- **AND** il indique les préconditions
- **AND** il indique les étapes
- **AND** il indique le résultat attendu
- **AND** il indique l'évidence à joindre.

### Requirement: Public build SHALL have automated leak checks

Le build public SHALL échouer si du contenu interne apparaît dans les artefacts
publiés.

#### Scenario: Public search index is checked

- **WHEN** la CI construit le site public
- **THEN** elle inspecte `site/search/search_index.json`
- **AND** elle échoue si une page hors allowlist apparaît
- **AND** elle échoue si un terme ou chemin interne denylisté apparaît.

#### Scenario: Public HTML and sitemap are checked

- **WHEN** la CI construit le site public
- **THEN** elle inspecte `site/sitemap.xml`
- **AND** elle inspecte les fichiers `site/**/*.html`
- **AND** elle échoue si un chemin interne ou une page hors allowlist est
  présent.
