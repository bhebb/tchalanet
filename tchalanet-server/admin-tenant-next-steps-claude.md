# Spec — Tenants V0 : provisioning minimal, détail tenant et configuration complète

**Projet :** `tchalanet-web` + backend tenant/platform  
**Surface :** Platform / Admin tenant  
**Acteurs :** `SUPER_ADMIN`, `TENANT_ADMIN`  
**Statut :** Proposed  
**Objectif :** guider la suite pour Claude/Codex après la stabilisation visuelle du provisioning tenant.

---

## 1. Décision produit principale

Le **superadmin ne doit pas configurer tout le tenant au moment de la création**.

Le flow V0 doit être :

```text
SUPER_ADMIN
  → crée un tenant avec le minimum viable
  → choisit un profil initial
  → peut créer/inviter un admin initial
  → peut voir toutes les informations dans le détail tenant

TENANT_ADMIN
  → première connexion
  → termine la configuration opérationnelle
  → prépare la vente réelle
```

Donc il ne faut pas surcharger l'écran `Provisionner un tenant`. Cet écran sert à créer une base propre, pas à remplir toute la configuration métier.

---

## 2. Séparation claire des concepts

### 2.1 Provisioning tenant

Le provisioning est l'acte de créer l'environnement tenant initial.

Il contient seulement les informations nécessaires pour créer un tenant fonctionnel avec des defaults sûrs.

### 2.2 Détail tenant superadmin

Le détail tenant est la page où le superadmin peut voir toutes les informations du tenant après création.

C'est là qu'on affiche :

```text
- identité
- statut
- type
- timezone
- currency
- commission par défaut
- adresse
- thème actif
- configuration interne
- administrateurs
- abonnement
- droits d’usage
- seller-terminals
- audit
```

### 2.3 Configuration tenant

La configuration tenant représente le comportement opérationnel du tenant.

Elle inclut :

```text
- langue & région
- calendrier commercial
- communication ticket
- reçu / documents
- règles opérationnelles
- commissions fines si besoin
```

### 2.4 Abonnements

`Abonnements` = subscriptions / plan commercial.

Exemples :

```text
- plan Starter / Pro / Network
- statut du plan
- période active
- renouvellement
- billing manuel en V0
- notes commerciales internes
```

### 2.5 Droits d’usage

`Droits d’usage` = entitlements / capacités activées pour un tenant.

Ce n'est pas la même chose que les permissions utilisateurs.

Exemples :

```text
- vente tickets activée
- résultats manuels activés
- promotions activées
- rapports avancés activés
- SMS activé
- WhatsApp activé
- nombre max de seller-terminals
- nombre max d’admins tenant
- nombre max de canaux de tirage
```

Résumé :

```text
Abonnement       = contrat / plan commercial
Droits d’usage   = features et limites réellement activées
Configuration    = comportement opérationnel du tenant
```

---

## 3. Écran superadmin — Provisionner un tenant

### 3.1 Objectif

Créer rapidement un tenant avec une configuration initiale cohérente.

### 3.2 Champs V0 à garder

```text
Identité
- code
- name
- type

Base régionale
- timezone
- currency

Commerce
- defaultCommissionRate

Profil initial
- profile

Admin initial
- initialAdminEmail optionnel
```

### 3.3 Champ à ajouter maintenant

Ajouter :

```text
defaultCommissionRate
```

Raison : la commission par défaut est centrale pour les ventes, les rapports et les seller-terminals.

Valeur par défaut recommandée :

```text
15.00 %
```

### 3.4 Champs à ne pas ajouter dans le provisioning V0

Ne pas mettre dans le formulaire initial :

```text
- adresse complète
- thème avancé
- header/footer reçu
- configuration SMS/WhatsApp détaillée
- calendrier commercial détaillé
- langues supportées détaillées
- canaux de tirage
- odds / barèmes
- limites détaillées
- seller-terminal creation
```

Ces éléments seront gérés après création, dans le détail tenant ou par l'admin tenant lors de l'onboarding.

---

## 4. Modèle backend concerné

### 4.1 Entité tenant

Référence actuelle :

```java
@Entity
@Table(name = "tenant")
public class TenantJpaEntity extends BaseEntity {
    private String code;
    private String name;
    private TenantType type;
    private String timezone;
    private String currency;
    private String defaultLanguage;
    private String defaultLocale;
    private TenantStatus status;
    private UUID addressId;
    private UUID activeThemeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private String config;

    private BigDecimal defaultCommissionRate;
}
```

### 4.2 Rôle des colonnes directes

Les colonnes directes servent à :

```text
- liste tenants
- filtre
- tri
- runtime rapide
- identity card
- bootstrap
```

Colonnes importantes :

```text
code
name
type
timezone
currency
defaultLanguage
defaultLocale
status
addressId
activeThemeId
defaultCommissionRate
```

### 4.3 Rôle du JSONB config

`config` contient les paramètres internes évolutifs :

```java
public record TenantInternalSettings(
    TenantInternalCommunicationConfig communication,
    TenantInternalDocumentConfig document,
    TenantInternalRules rules,
    TenantInternalLocaleConfig locale
) {}
```

Il contient notamment :

```text
- communication
- document
- rules
- locale
```

---

## 5. Locale : décision V0

### 5.1 Defaults critiques

Pour Haïti V0 :

```text
defaultLanguage = fr
defaultLocale = fr-HT
supportedLanguages = [fr, ht, en]
fallbackLanguage = fr
```

### 5.2 Provisioning

Dans l'écran superadmin, ne pas exposer tous les champs de locale.

Le profil initial doit appliquer les defaults.

UI visible :

```text
Profil : Haïti Loterie
Locale prévue : fr-HT
Langues prévues : fr, ht, en
```

### 5.3 Source de vérité

Décision recommandée :

```text
config.locale = source métier
colonnes defaultLanguage/defaultLocale = copie rapide/dénormalisée
```

Donc si `config.locale.defaultLanguage` ou `config.locale.defaultLocale` change, le service tenant doit synchroniser les colonnes :

```java
entity.setDefaultLanguage(settings.locale().defaultLanguage());
entity.setDefaultLocale(settings.locale().defaultLocale());
```

### 5.4 Adapter existant

L'adapter suivant est cohérent avec le modèle V0 :

```java
@Component
@RequiredArgsConstructor
public class TenantLocaleApiAdapter implements TenantLocaleApi {

    private static final String FALLBACK_LANGUAGE = "fr";
    private static final Locale FALLBACK_LOCALE = Locale.forLanguageTag("fr-HT");
    private static final List<String> FALLBACK_SUPPORTED = List.of("fr", "ht", "en");

    private final TenantConfigReader reader;

    @Override
    public Locale resolveDefaultLocale(TenantId tenantId) {
        var tag = locale(tenantId)
            .map(TenantInternalLocaleConfig::defaultLocale)
            .filter(s -> s != null && !s.isBlank())
            .orElse(null);
        return tag == null ? FALLBACK_LOCALE : Locale.forLanguageTag(tag);
    }

    @Override
    public String resolveDefaultLanguage(TenantId tenantId) {
        return locale(tenantId)
            .map(TenantInternalLocaleConfig::defaultLanguage)
            .filter(s -> s != null && !s.isBlank())
            .orElse(FALLBACK_LANGUAGE);
    }

    @Override
    public List<String> resolveSupportedLanguages(TenantId tenantId) {
        return locale(tenantId)
            .map(TenantInternalLocaleConfig::effectiveSupportedLanguages)
            .filter(l -> !l.isEmpty())
            .orElse(FALLBACK_SUPPORTED);
    }
}
```

À vérifier plus tard : éviter toute divergence durable entre `config.locale` et les colonnes `defaultLanguage/defaultLocale`.

---

## 6. Configuration tenant : représentation UI

### 6.1 Où représenter la configuration ?

Pas dans l'écran initial de création.

Elle doit être visible dans :

```text
Platform > Tenants > Détail tenant > Configuration
```

et modifiable principalement dans :

```text
Tenant Admin > Paramètres / Onboarding
```

### 6.2 Sections de configuration

#### Langue & région

JSON :

```json
{
  "locale": {
    "defaultLanguage": "fr",
    "defaultLocale": "fr-HT",
    "supportedLanguages": ["fr", "ht", "en"],
    "fallbackLanguage": "fr"
  }
}
```

UI :

```text
Langue par défaut       fr
Locale par défaut       fr-HT
Langues supportées      fr, ht, en
Langue fallback         fr
```

#### Calendrier commercial

JSON :

```json
{
  "rules": {
    "businessCalendar": {
      "defaultOpen": true,
      "closedWeekdays": [],
      "holidaySalesAllowed": false
    }
  }
}
```

UI :

```text
Ouvert par défaut                 Oui / Non
Jours fermés                      Lundi, Mardi, ...
Vente autorisée les jours fériés  Oui / Non
```

#### Communication ticket

JSON :

```json
{
  "communication": {
    "buyerTicketDelivery": {
      "sms": {
        "enabled": true,
        "amount": 5.00,
        "currency": "HTG",
        "paidBy": "BUYER"
      },
      "whatsapp": {
        "enabled": true,
        "amount": 5.00,
        "currency": "HTG",
        "paidBy": "BUYER"
      },
      "email": {
        "enabled": true,
        "amount": 0.00,
        "currency": "HTG",
        "paidBy": "TENANT"
      }
    }
  }
}
```

UI :

```text
Livraison ticket acheteur

SMS
- Activé
- Frais : 5 HTG
- Payé par : Acheteur

WhatsApp
- Activé
- Frais : 5 HTG
- Payé par : Acheteur

Email
- Activé
- Frais : 0 HTG
- Payé par : Tenant
```

Note V0 : si SMS/WhatsApp ne sont pas supportés opérationnellement, les masquer ou les afficher en disabled avec mention “non disponible V0”.

#### Reçu / documents

JSON :

```json
{
  "document": {
    "receipt": {
      "enabled": true,
      "displayName": "CHEZ Toto",
      "headerMessage": "Lotto officiel - Bonne chance",
      "footerMessage": "Merci de votre confiance",
      "defaultPaperSize": "RECEIPT_80MM",
      "showQrCode": true,
      "showSellerName": true,
      "showOutletName": true,
      "showPotentialPayout": true,
      "defaultTemplateKey": "sales.ticket.receipt.v1"
    }
  }
}
```

UI :

```text
Reçu activé
Nom affiché sur reçu
Message d’en-tête
Message de pied
Format papier par défaut
Afficher QR code
Afficher nom vendeur
Afficher nom outlet
Afficher gain potentiel
Template par défaut
```

Note V0 : comme le modèle simplifie outlet, `showOutletName` peut rester dans la config mais ne doit pas bloquer l'écran.

---

## 7. Adresse du tenant

### 7.1 Création

L'adresse est optionnelle dans l'écran de provisioning V0.

Elle ne doit pas bloquer la création.

### 7.2 Détail tenant

Dans le détail tenant, afficher un bloc :

```text
Adresse
- pays
- ville
- commune
- rue / zone
- téléphone principal optionnel
```

Si absente :

```text
Adresse non renseignée
[Ajouter une adresse]
```

### 7.3 Backend

`TenantJpaEntity.addressId` référence l'adresse.

Ne pas inventer des champs adresse dans l'entité tenant si le modèle address existe déjà.

---

## 8. Thème du tenant

### 8.1 Création

Le provisioning applique le thème par défaut du profil.

Ne pas exposer un éditeur de thème dans le formulaire initial.

### 8.2 Détail tenant

Afficher :

```text
Thème actif
- nom / code du thème
- mode si disponible
- aperçu rapide
- action : Modifier
```

### 8.3 Backend

`TenantJpaEntity.activeThemeId` référence le thème actif.

---

## 9. Commission

### 9.1 Provisioning

Ajouter `defaultCommissionRate` dans l'écran de provisioning.

UI :

```text
Commission par défaut du tenant
15.00 %
```

### 9.2 Usage métier

Cette commission sert de défaut pour les seller-terminals.

Règle :

```text
sellerTerminal.commissionRate si défini
sinon tenant.defaultCommissionRate
```

### 9.3 Détail tenant

Dans l'onglet Aperçu ou Commerce :

```text
Commission par défaut : 15.00 %
```

---

## 10. Types de tenant

### 10.1 Types proposés

```text
BORLETTE
RESEAU
AMBULANT
```

### 10.2 BORLETTE

Type standard.

Définition :

```text
Banque borlette ou opérateur standard qui gère un ou plusieurs vendeurs.
```

Peut avoir :

```text
1 vendeur
10 vendeurs
60 vendeurs
100 vendeurs
```

Cas important :

```text
Une personne avec 60 vendeurs = BORLETTE
```

Le type décrit l'organisation opérationnelle, pas seulement la forme légale.

### 10.3 AMBULANT

Définition :

```text
Vendeur solo ou très petite activité mobile.
```

Usage :

```text
- vendeur indépendant
- petit marchand sans équipe
- un téléphone / très peu de seller-terminals
```

En V0, `AMBULANT` peut appliquer un profil plus simple :

```text
max seller-terminals bas
admin minimal
rapports simples
```

### 10.4 RESEAU

Définition :

```text
Organisation qui supervise plusieurs points, zones, borlettes ou sous-opérateurs.
```

En V0, si les notions multi-zones/sous-tenants ne sont pas prêtes, `RESEAU` peut rester un type avancé/futur mais visible.

### 10.5 Aide UI

Dans le selector de type tenant, ajouter une aide courte :

```text
Borlette
Pour une banque borlette ou un opérateur qui gère plusieurs vendeurs.

Réseau
Pour une organisation qui supervise plusieurs points, zones ou opérateurs.

Ambulant
Pour un vendeur indépendant ou une petite activité mobile.
```

---

## 11. Détail tenant superadmin

### 11.1 Route recommandée

```text
/app/platform/tenants/:tenantId
```

ou :

```text
/app/platform/tenants/:tenantId/overview
```

### 11.2 Objectif

Le superadmin doit pouvoir voir toutes les informations du tenant après création, sans devoir tout remplir au moment du provisioning.

### 11.3 Header

```text
Grand Pari Haïti
GPH-7729-LOT
BORLETTE · ACTIVE · HTG · America/Port-au-Prince

Actions :
- Accéder comme admin
- Modifier
- Suspendre
- Archiver
```

### 11.4 Layout

Pattern recommandé :

```text
AdminPageShell
  TenantDetailHeader
  tabs / sections
  right rail optionnel
```

### 11.5 Onglets recommandés

```text
Aperçu
Configuration
Administrateurs
Abonnement
Droits d’usage
Seller-terminals
Audit
```

---

## 12. Onglet Aperçu

Afficher une synthèse :

```text
Identité
- code
- name
- type
- status

Région
- timezone
- currency
- defaultLanguage
- defaultLocale

Commerce
- defaultCommissionRate

Adresse
- résumé adresse ou “non renseignée”

Thème
- thème actif

Configuration
- statut complète / incomplète
- dernière modification
```

---

## 13. Onglet Configuration

Afficher le contenu de `TenantInternalSettings` en sections.

En V0, lecture seule acceptable si l'édition n'est pas prête.

Sections :

```text
Langue & région
Calendrier commercial
Communication
Reçu / documents
Règles
```

Actions :

```text
Modifier
Accéder comme admin pour support
Copier config JSON en dev/support si nécessaire
```

---

## 14. Onglet Administrateurs

Afficher :

```text
- nom
- email
- statut invitation
- rôle
- dernière connexion
- actions : inviter, désactiver, renvoyer invitation
```

---

## 15. Onglet Abonnement

Afficher :

```text
Plan
- Starter / Pro / Network
- statut
- date début
- date fin
- renouvellement
- montant si supporté
- notes internes
```

Si billing réel absent en V0 :

```text
Plan manuel
Statut manuel
Notes superadmin
```

---

## 16. Onglet Droits d’usage

Afficher les entitlements :

```text
Fonctionnalités activées
- vente tickets
- résultats manuels
- promotions
- rapports avancés
- SMS
- WhatsApp
- seller-terminals

Limites
- max seller-terminals
- max admins
- max draw channels
- max tickets/jour si applicable
```

---

## 17. Onglet Seller-terminals

Afficher :

```text
- code
- displayName
- statut
- commission effective
- dernière activité
- actions : reset PIN, bloquer, modifier
```

Important : dans le modèle V0, le seller-terminal représente l'acteur de vente principal.

---

## 18. Onglet Audit

Afficher l'historique :

```text
- tenant créé
- statut changé
- admin invité
- config modifiée
- support access démarré
- suspension
- archivage
- reset PIN seller-terminal
```

---

## 19. Navigation recommandée

### 19.1 Sidebar superadmin V0 simplifiée

Sous `Tenants`, éviter de surcharger.

Proposition V0 :

```text
Tenants
  Tous les tenants
  Créer un tenant
  Administrateurs
```

Puis dans le détail tenant, utiliser les onglets :

```text
Aperçu
Configuration
Abonnement
Droits d’usage
Seller-terminals
Audit
```

### 19.2 Variante si on garde des pages transversales

Si besoin de pages globales :

```text
Tenants
  Tous les tenants
  Créer un tenant
  Configurations
  Administrateurs
  Plans & abonnements
  Droits d’usage
```

Mais la préférence V0 reste : **tout concentrer dans le détail tenant** après sélection d'un tenant.

---

## 20. Onboarding tenant admin après première connexion

### 20.1 Objectif

L'admin tenant doit terminer la configuration opérationnelle.

### 20.2 Route possible

```text
/app/admin/onboarding
```

ou :

```text
/app/admin/settings/setup
```

### 20.3 Message

```text
Bienvenue. Votre tenant a été créé par Tchalanet.
Terminez la configuration avant d’ouvrir la vente.
```

### 20.4 Étapes

```text
1. Vérifier les infos du tenant
2. Compléter l’adresse
3. Configurer le reçu
4. Configurer les tirages/canaux
5. Configurer les règles de vente
6. Créer les seller-terminals
7. Valider / ouvrir la vente
```

### 20.5 Readiness bootstrap

Le runtime bootstrap admin tenant pourrait exposer :

```ts
tenantSetup: {
  status: 'INCOMPLETE' | 'READY';
  requiredSteps: [
    'ADDRESS',
    'DOCUMENT_RECEIPT',
    'DRAW_CHANNELS',
    'SELLER_TERMINALS',
    'LIMITS',
    'ODDS'
  ];
}
```

---

## 21. Actions superadmin sensibles

Certaines actions depuis le détail tenant doivent passer par un dialog sensible :

```text
- Accéder comme admin tenant
- Suspendre tenant
- Archiver tenant
- Changer plan/droits sensibles
- Reset PIN seller-terminal
```

Règle :

```text
motif requis
confirmation explicite
audit
trace id si erreur
```

---

## 22. Erreurs et notifications

Ne pas afficher toutes les erreurs techniques en gros blocs rouges.

Règle :

```text
- erreur page bloquante → TchErrorPanel page-level
- erreur section → TchErrorPanel dans la section
- warning non bloquant → TchNotice warning
- succès persistant → TchNotice success
- détails techniques → repliés + bouton Copier
```

Ne jamais afficher :

```text
[object Object]
```

Toujours mapper les erreurs via une fonction dédiée :

```ts
errorToMessage(error)
errorToTechnicalDetails(error)
```

---

## 23. Roadmap recommandée après provisioning

### Étape 1 — Finaliser provisioning

```text
- ajouter defaultCommissionRate
- success TchNotice + NextSteps
- bouton disabled explicite si invalide
```

### Étape 2 — Liste tenants

```text
- filtres fonctionnels
- pagination
- actions par ligne via mat-menu
- loading/error/empty
- CTA vers créer tenant
```

### Étape 3 — Détail tenant

```text
- route détail
- onglet Aperçu
- onglet Configuration lecture seule
- onglet Administrateurs minimum
```

### Étape 4 — Onboarding admin tenant

```text
- première connexion
- setup checklist
- paramètres tenant
```

### Étape 5 — Abonnements / droits d’usage

```text
- modèle simple V0
- affichage dans détail tenant
- édition superadmin si nécessaire
```

---

## 24. Critères d’acceptation

### Provisioning

```text
- defaultCommissionRate visible et envoyé au backend
- champs minimalistes conservés
- pas d’adresse/theme/config détaillée dans le formulaire initial
- success visible après création
```

### Détail tenant

```text
- superadmin peut voir toutes les infos clés du tenant
- onglets ou sections disponibles
- configuration interne affichée lisiblement
- adresse/thème/commission visibles
- abonnement/droits d’usage visibles même si V0 manuel
```

### Tenant admin onboarding

```text
- tenant créé par superadmin peut être incomplet opérationnellement
- admin tenant voit une checklist de configuration à compléter
- defaults sûrs appliqués par profil
```

---

## 25. Non-goals immédiats

```text
- billing réel complet
- éditeur de thème avancé
- support SMS/WhatsApp complet si non implémenté
- sous-tenants / réseau avancé
- refonte dashboard
- toutes les pages globales transversales
```

---

## 26. Résumé décisionnel

```text
Provisioning superadmin = minimum + commission + profil.
Détail tenant superadmin = toutes les informations visibles.
Admin tenant first login = configuration opérationnelle complète.

Abonnement = plan commercial.
Droits d’usage = features et limites activées.
Configuration = rules + locale + communication + document + commission/règles opérationnelles.

BORLETTE = opérateur standard, même avec 60 vendeurs.
AMBULANT = vendeur solo / micro activité mobile.
RESEAU = structure multi-points / zones / sous-opérateurs.
```
