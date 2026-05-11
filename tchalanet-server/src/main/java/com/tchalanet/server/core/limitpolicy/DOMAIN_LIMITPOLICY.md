# Core – LimitPolicy (V1)

## 1. Vision

`LimitPolicy` évalue les limites runtime configurées et maintient les projections d’exposition nécessaires aux règles stateful.

Il répond à une question précise :

> Est-ce que cette opération respecte les limites configurées pour ce tenant, ce canal, cet outlet ou cet agent ?

`LimitPolicy` ne vend rien, ne paye rien, n’approuve rien.

Il produit uniquement un résultat d’évaluation :

- `ALLOW`
- `WARN`
- `REQUIRE_APPROVAL`
- `BLOCK`

---

## 2. Ce que LimitPolicy fait / ne fait pas

### LimitPolicy fait

- Évaluer des règles configurées par scope
- Résoudre la règle la plus spécifique applicable
- Lire les facts d’exposition nécessaires aux règles stateful
- Produire des breaches détaillés
- Maintenir `draw_exposure` après les ventes confirmées

### LimitPolicy ne fait pas

- Exécuter une vente
- Exécuter un payout
- Décider une approbation humaine
- Gérer un workflow métier
- Gérer l’UI
- Remplacer `Autonomy`

---

## 3. Responsabilités

Le domaine a trois responsabilités distinctes.

### 3.1 Rule configuration

Les règles disponibles sont définies dans le code via `RuleKey` et les evaluators.

La base de données ne définit pas de nouvelles règles.

Elle configure seulement les règles existantes via `limit_assignment`.

### 3.2 Runtime evaluation

Le runtime évalue une opération en cours, par exemple une vente.

Il prend :

- un `LimitContext`
- les `LimitAssignment` applicables
- les facts `draw_exposure`
- les evaluators disponibles

Il retourne un `LimitEvaluationResult`.

### 3.3 Exposure accounting

`draw_exposure` est une projection dérivée alimentée après commit des ventes.

Elle sert aux règles stateful comme :

- mise totale déjà vendue sur un numéro
- payout potentiel déjà exposé sur un numéro
- nombre de ventes/lignes déjà enregistrées sur une sélection

---

## 4. Architecture

```text
core.limitpolicy
├── domain
│   ├── model
│   │   ├── LimitAssignment
│   │   ├── LimitScopeRef
│   │   ├── LimitContext
│   │   ├── LimitLineContext
│   │   ├── EffectiveLimitRule
│   │   ├── EffectiveLimits
│   │   ├── LimitFactsSnapshot
│   │   ├── LimitBreach
│   │   └── LimitEvaluationResult
│   ├── resolver
│   │   └── LimitResolver
│   ├── engine
│   │   └── LimitEvaluationEngine
│   └── rule
│       ├── LimitRuleEvaluator
│       ├── LimitRuleParams
│       └── evaluators/*
│
├── application
│   ├── command
│   │   ├── model
│   │   └── handler
│   │       ├── assignment
│   │       └── exposure
│   ├── query
│   │   ├── model
│   │   │   ├── assignment
│   │   │   ├── evaluation
│   │   │   ├── exposure
│   │   │   └── rules
│   │   └── handler
│   │       ├── assignment
│   │       ├── evaluation
│   │       ├── exposure
│   │       └── rules
│   ├── port.out
│   │   ├── assignment
│   │   └── exposure
│   └── service
│
└── infra
    ├── web
    │   └── admin
    ├── persistence
    │   ├── assignment
    │   └── exposure
    ├── event
    └── config

5. Rule model

Les règles sont code-defined.

Cela veut dire :

RuleKey + Evaluator = règle exécutable
LimitAssignment = configuration DB de cette règle

Il n’y a pas de moteur dynamique de règles en V1.

Il n’y a plus de table limit_definition.

6. RuleKey

RuleKey représente les règles que le backend sait exécuter.

Exemple V1 :

public enum RuleKey {

    // Montant maximum autorisé pour une seule ligne de ticket.
    MAX_STAKE_PER_LINE,

    // Nombre maximum de lignes autorisées sur un ticket.
    MAX_LINES_PER_TICKET,

    // Montant total maximum autorisé pour tout le ticket.
    MAX_STAKE_PER_TICKET,

    // Mise totale maximum déjà exposée sur une sélection pour un tirage donné.
    MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,

    // Gain potentiel total maximum déjà exposé sur une sélection pour un tirage donné.
    MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW,

    // Mise maximum par type de pari dans un même ticket.
    MAX_STAKE_PER_BET_TYPE_PER_TICKET,

    // Mise maximum sur une même sélection dans un même ticket.
    MAX_STAKE_PER_SELECTION_PER_TICKET,

    // Gain potentiel maximum pour tout le ticket.
    MAX_POTENTIAL_PAYOUT_PER_TICKET,

    // Gain potentiel maximum pour une seule ligne.
    MAX_POTENTIAL_PAYOUT_PER_LINE,

    // Nombre maximum de ventes sur une sélection pour un tirage.
    MAX_SALES_COUNT_PER_SELECTION_PER_DRAW,

    // Bloque une ou plusieurs sélections pour un tirage.
    BLOCK_SELECTION_PER_DRAW,

    // Bloque complètement un type de pari.
    BLOCK_BET_TYPE
}

Chaque RuleKey doit avoir :

une valeur dans l’enum,
un LimitRuleEvaluator,
une entrée dans le catalogue JSON des règles disponibles,
des tests.
7. LimitAssignment

limit_assignment stocke la configuration réelle d’une règle pour un scope.

Un assignment répond à la question :

Pour ce scope, cette règle est-elle active, avec quel comportement et quels paramètres ?

Champs principaux
id
tenant_id
rule_key
scope_type
scope_id
enabled
on_breach
params
starts_at
ends_at
version
created_at
updated_at
deleted_at
Exemple
{
  "ruleKey": "MAX_STAKE_PER_TICKET",
  "scopeType": "OUTLET",
  "scopeId": "outlet-uuid",
  "enabled": true,
  "onBreach": "BLOCK",
  "params": {
    "valueCents": 500000
  }
}

Cela signifie :

Pour cet outlet, un ticket ne peut pas dépasser 5 000 HTG.
Si la limite est dépassée, l’opération est bloquée.

8. Scopes V0

Pour la V0, les scopes supportés sont :

TENANT
DRAWCHANNEL
OUTLET
AGENT

TERMINAL est volontairement exclu de la V0.

Il pourra être ajouté plus tard sans changer le modèle général.

Hiérarchie de résolution
AGENT
↓
OUTLET
↓
DRAWCHANNEL
↓
TENANT

La règle active la plus spécifique gagne.

Exemple :

TENANT      MAX_STAKE_PER_TICKET = 10 000 HTG
OUTLET      MAX_STAKE_PER_TICKET = 5 000 HTG
AGENT       MAX_STAKE_PER_TICKET = 2 000 HTG

Pour une vente faite par cet agent, la limite effective est :

AGENT = 2 000 HTG
9. LimitScopeRef

LimitScopeRef représente un scope configurable.

public sealed interface LimitScopeRef
    permits LimitScopeRef.TenantScope,
            LimitScopeRef.OutletScope,
            LimitScopeRef.AgentScope,
            LimitScopeRef.DrawChannelScope {

  static TenantScope tenant(TenantId tenantId) {
    return new TenantScope(tenantId);
  }

  static OutletScope outlet(OutletId outletId) {
    return new OutletScope(outletId);
  }

  static AgentScope agent(UserId userId) {
    return new AgentScope(userId);
  }

  static DrawChannelScope drawChannel(DrawChannelId drawChannelId) {
    return new DrawChannelScope(drawChannelId);
  }

  record TenantScope(TenantId tenantId) implements LimitScopeRef {}

  record OutletScope(OutletId outletId) implements LimitScopeRef {}

  record AgentScope(UserId userId) implements LimitScopeRef {}

  record DrawChannelScope(DrawChannelId drawChannelId) implements LimitScopeRef {}
}
10. LimitContext

LimitContext décrit l’opération en cours.

Il ne configure rien.

Il dit seulement :

Voici la vente ou l’opération que l’on veut évaluer.

Exemple :

public record LimitContext(
    TenantId tenantId,
    OutletId outletId,
    UserId userId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    Instant now,
    List<LimitLineContext> lines
) {}

LimitContext.scopes() retourne les scopes applicables à l’opération :

TENANT
DRAWCHANNEL
OUTLET
AGENT

selon les IDs présents dans le contexte.

11. LimitLineContext

LimitLineContext représente une ligne de ticket déjà préparée par Sales.

public record LimitLineContext(
    BetType betType,
    String selectionKey,
    long stakeCents,
    long potentialPayoutCents
) {}

Important :

selectionKey doit déjà être canonique.
stakeCents est exprimé en centimes.
potentialPayoutCents est exprimé en centimes.
12. Runtime flow
Sales prépare une vente
        ↓
Sales construit LimitContext
        ↓
EvaluateLimitPolicyQuery
        ↓
LimitAssignmentReaderPort charge les assignments applicables
        ↓
LimitResolver choisit les règles effectives
        ↓
ExposureFactsReaderPort charge draw_exposure
        ↓
LimitEvaluationEngine exécute les evaluators
        ↓
LimitEvaluationResult
        ↓
Sales décide :
  - continuer
  - afficher warning
  - appeler Autonomy
  - rejeter

Étapes détaillées :

Sales prépare une vente.
Sales construit LimitContext.
EvaluateLimitPolicyQuery est appelée.
Le handler charge les LimitAssignment applicables aux scopes du contexte.
LimitResolver choisit une seule règle effective par RuleKey.
ExposureFactsReaderPort charge les facts draw_exposure.
LimitEvaluationEngine exécute les evaluators.
Le résultat est ALLOW, WARN, REQUIRE_APPROVAL ou BLOCK.
Si BLOCK ou REQUIRE_APPROVAL, Sales appelle Autonomy.
Après commit de la vente, TicketPlacedEvent alimente draw_exposure.
13. LimitResolver

LimitResolver reçoit :

List<LimitAssignment>
LimitContext

Il retourne :

EffectiveLimits

Son rôle :

ignorer les assignments disabled,
ignorer les assignments supprimés,
ignorer les assignments hors fenêtre temporelle,
vérifier si le scope s’applique au contexte,
garder la règle la plus spécifique pour chaque RuleKey.
Score de spécificité V0
AGENT       = 60
OUTLET      = 50
DRAWCHANNEL = 30
TENANT      = 10

Le score n’est pas une règle métier complexe.

Il sert seulement à choisir le scope le plus spécifique.

14. EffectiveLimitRule

Après résolution, il ne reste qu’une règle effective par RuleKey.

public record EffectiveLimitRule(
    RuleKey ruleKey,
    BreachOutcome onBreach,
    LimitScopeRef appliedScope,
    LimitAssignmentId assignmentId,
    JsonNode params
) {}

Exemple :

MAX_STAKE_PER_TICKET
appliedScope = OUTLET O1
params = { "valueCents": 500000 }
onBreach = BLOCK

Cela signifie :

Pour cette opération, la règle MAX_STAKE_PER_TICKET applicable vient de l’outlet O1.

15. LimitEvaluationEngine

LimitEvaluationEngine ne choisit pas les règles.

Il reçoit déjà les règles finales :

EffectiveLimits
LimitFactsSnapshot
LimitContext

Il exécute les evaluators correspondants.

Flow interne :

for each EffectiveLimitRule:
  evaluator = evaluators[rule.ruleKey]
  breaches += evaluator.evaluate(rule, facts, context)
16. LimitRuleEvaluator

Chaque evaluator implémente une règle.

public interface LimitRuleEvaluator {

  RuleKey supports();

  List<LimitBreach> evaluate(
      EffectiveLimitRule rule,
      LimitFactsSnapshot facts,
      LimitContext context
  );
}

Les evaluators ne connaissent pas :

la DB,
les repositories,
les controllers,
les assignments bruts,
Sales,
Payout,
Autonomy.

Ils reçoivent seulement :

EffectiveLimitRule
LimitFactsSnapshot
LimitContext
17. Résultat runtime

LimitEvaluationResult contient :

outcome
breaches

Le résultat global est le plus sévère des breaches.

Ordre de sévérité :

ALLOW < WARN < REQUIRE_APPROVAL < BLOCK
18. LimitBreach

Un breach décrit précisément quelle règle a été dépassée.

public record LimitBreach(
    RuleKey ruleKey,
    BreachOutcome outcome,
    LimitScopeRef appliedScope,
    String code,
    String messageKey,
    Long limitValue,
    Long currentValue,
    Long deltaValue
) {}

Les valeurs sont volontairement en Long.

Selon la règle, elles peuvent représenter :

des centimes,
un count,
une quantité.

Le format UI détaillé pourra être enrichi plus tard avec un valueKind.

19. Règles stateless

Les règles stateless utilisent uniquement le ticket en cours.

Elles ne lisent pas draw_exposure.

Exemples :

MAX_STAKE_PER_LINE
MAX_LINES_PER_TICKET
MAX_STAKE_PER_TICKET
MAX_STAKE_PER_BET_TYPE_PER_TICKET
MAX_STAKE_PER_SELECTION_PER_TICKET
MAX_POTENTIAL_PAYOUT_PER_TICKET
MAX_POTENTIAL_PAYOUT_PER_LINE
BLOCK_SELECTION_PER_DRAW
BLOCK_BET_TYPE
20. Règles stateful / exposure

Les règles stateful utilisent draw_exposure.

Exemples :

MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW
MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW
MAX_SALES_COUNT_PER_SELECTION_PER_DRAW

Ces règles comparent :

current exposure + current ticket delta

à la limite configurée.

21. draw_exposure

draw_exposure est une projection dérivée utilisée par LimitPolicy.

Elle représente :

le risque potentiel créé par les ventes confirmées

Elle ne représente pas :

les payouts réels,
les payouts approuvés,
les tickets déjà payés,
le reste à payer.

Elle est alimentée uniquement par les ventes confirmées.

22. Exposure flow

draw_exposure est alimentée après commit de la vente.

Flow :

Sales commit ticket
        ↓
TicketPlacedEvent
        ↓
LimitPolicyEventsListener
        ↓
ApplyTicketExposureCommand
        ↓
ProcessedEventPort.markProcessedIfAbsent
        ↓
ExposureProjectorPort.applyTicketSold
        ↓
increment_draw_exposure(...)
23. Projection multi-scope

Pour chaque ticket vendu, draw_exposure est incrémentée sur plusieurs scopes :

TENANT
DRAWCHANNEL
OUTLET
AGENT

Exemple :

Ticket vendu par agent A1
dans outlet O1
sur drawChannel NY_MIDI
pour tenant T1

La projection écrit les facts pour :

TENANT T1
DRAWCHANNEL NY_MIDI
OUTLET O1
AGENT A1

Ainsi, une règle tenant, channel, outlet ou agent peut lire ses propres facts.

24. Pourquoi exposure est multi-scope

Si on écrivait seulement sur le scope le plus spécifique, par exemple AGENT, alors les règles TENANT ou OUTLET ne verraient jamais l’exposition.

La projection doit donc écrire sur tous les scopes applicables.

Ensuite, au runtime :

LimitResolver choisit appliedScope
Evaluator lit facts[appliedScope]
25. LimitFactsSnapshot

LimitFactsSnapshot contient les facts chargés depuis draw_exposure.

La clé contient :

scope
betType
selectionKey

Exemple :

facts.fact(
    rule.appliedScope(),
    betType,
    selectionKey
)

Cela garantit que l’evaluator lit les facts du scope effectivement choisi par le resolver.

26. Idempotence

L’application de l’exposition doit être idempotente.

Le handler utilise :

ProcessedEventPort
handler_key = limitpolicy.exposure
event_id = TicketPlacedEvent.eventId

Flow :

markProcessedIfAbsent(handler_key, event_id)
  false -> noop
  true  -> apply exposure

Cela évite de doubler l’exposition en cas de retry ou de replay.

27. Canonicalisation

Toutes les écritures de selection_key doivent passer par :

SelectionKeyCanonicalizer

Exemples :

MATCH_1_2D       -> 05
MATCH_2_2D       -> 12-34
MATCH_3_2D       -> 12-34-56
MARRIAGE_2D2D    -> 12-34
LOTTO3_3D        -> 123
LOTTO4_PATTERN   -> 12**

Règle importante :

La canonicalisation doit être appliquée avant d’écrire dans draw_exposure.

Sinon les facts peuvent être fragmentés entre plusieurs clés équivalentes.

28. Consistency model

draw_exposure est éventuellement consistante.

Flow normal :

vente acceptée
commit
event after commit
projection exposure

Cela signifie qu’entre la vente et l’application de l’exposition, une autre vente concurrente peut lire un état légèrement ancien.

Pour V1, ce comportement est acceptable.

29. Source of truth

draw_exposure n’est pas la source de vérité transactionnelle.

Sources de vérité :

tickets
ticket_lines
payouts

draw_exposure est une projection optimisée pour l’évaluation runtime.

Elle doit pouvoir être reconstruite plus tard à partir des événements ou des données sources.

## 30. Payouts

draw_exposure ne doit pas recevoir les événements de payout en V1.

Les payouts réels représentent un autre concept :

montants demandés
montants approuvés
montants payés
montants rejetés

Si nécessaire, une projection séparée sera créée plus tard :

draw_payout_summary

ou :

draw_payout_exposure

## 31. Annulations / void tickets

En V1, les annulations peuvent être traitées de deux façons.

Option simple

Ne pas décrémenter draw_exposure.

Le système reste conservateur : l’exposition peut être un peu trop haute.

Option exacte

Écouter :

TicketCancelledEvent
TicketVoidedEvent

et appliquer un delta négatif.

Ce n'est pas requis pour V1.

## 32. Admin API

Le controller admin expose :

GET    /admin/policies/limits/rules
GET    /admin/policies/limits/assignments
PUT    /admin/policies/limits/assignments
DELETE /admin/policies/limits/assignments/{id}
GET    /admin/policies/limits/exposure
Règles
Le controller reste thin.
Le controller construit les commands/queries.
Le controller ne lit pas la DB.
Le controller ne contient pas de logique métier.
Le controller utilise TchRequestContext pour le tenant.
Le client ne fournit jamais tenant_id.
33. Catalogue JSON des règles

Le catalogue JSON décrit les règles disponibles pour l’admin UI.

Il ne remplace pas les evaluators.

RuleKey + Evaluator = logique exécutable
rules.v1.json = metadata UI/documentation
LimitAssignment = configuration DB réelle

Fichier :

src/main/resources/limitpolicy/rules.v1.json

Il contient :

ruleKey
label
description
defaultOutcome
category
stateless
paramsTemplate
34. Tables principales
limit_assignment

Stocke les règles configurées.

id
tenant_id
rule_key
scope_type
scope_id
enabled
on_breach
params
starts_at
ends_at
version
created_at
updated_at
deleted_at

Unique actif :

tenant_id + rule_key + scope_type + scope_id
WHERE deleted_at IS NULL
draw_exposure

Stocke l’exposition agrégée par draw, scope, bet type et sélection.

id
tenant_id
draw_id
scope_type
scope_id
bet_type
selection_key
stake_total
sales_count
potential_payout_total
last_event_id
last_event_at
version
created_at
updated_at
deleted_at

Unique actif :

tenant_id + draw_id + scope_type + scope_id + bet_type + selection_key
WHERE deleted_at IS NULL
35. RLS

tenant_id est géré par le contexte et la base.

Règles :

Aucun tenant_id ne vient du client.
Les queries reposent sur TchRequestContext.
Les repositories s’appuient sur RLS.
Les events portent les IDs nécessaires pour les flows async.
Les batchs/retry devront binder un contexte explicitement.
36. Relation avec Autonomy

LimitPolicy ne décide pas si un blocage peut être overridé.

Il dit seulement :

Cette opération est ALLOW/WARN/REQUIRE_APPROVAL/BLOCK

Si le résultat impose une décision humaine, le caller appelle Autonomy.

Flow :

LimitPolicy BLOCK / REQUIRE_APPROVAL
        ↓
ResolveAutonomyQuery
        ↓
Sales/Payout décide reject / pending approval / auto override
37. Mental model
RuleKey
= règle codée dans le backend

LimitAssignment
= configuration d’une règle pour un scope

LimitContext
= opération en cours

LimitResolver
= choisit la règle la plus spécifique

EffectiveLimitRule
= règle finale applicable

LimitEvaluationEngine
= exécute les evaluators

LimitFactsSnapshot
= facts draw_exposure chargés pour l’évaluation

draw_exposure
= projection de risque potentiel créée par les ventes confirmées

TL;DR :

LimitPolicy says:
  “This operation exceeds configured limits.”

Autonomy says:
  “Can someone decide anyway?”
```
