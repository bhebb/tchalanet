---
name: rule-management
description: >
  Use when creating, modifying, or debugging DecisionSet JSON files in tchalanet-edge-service/rules/,
  implementing rule evaluation logic with json-rules-engine, or working with the
  /api/rules/* endpoints. Covers DecisionSet format, conditions syntax, event structure,
  and the loadDecisionSetForEvent / evaluateDecisionSet APIs.
---

# Rule Management — tchalanet-edge-service

## Concept

Un **DecisionSet** est un fichier JSON chargé dynamiquement depuis `rules/` en fonction du type d'événement reçu. Il contient une liste de règles (`decisions`) évaluées par `json-rules-engine`.

---

## Format DecisionSet

```typescript
// src/core/decision-types.ts
export interface DecisionSet {
  name: string; // ex. "ticket.created"
  attributes?: DecisionAttribute[]; // metadata optionnelle
  decisions: Decision[]; // liste de règles
}

export interface Decision {
  conditions: any; // format json-rules-engine (all/any/not)
  event: DecisionEvent;
}

export interface DecisionEvent {
  type: string; // ex. "send_notification"
  params?: any; // payload de l'événement émis
}
```

---

## Convention de nommage

```
rules/
├─ ticket.created.json      # chargé pour eventType = "ticket.created"
├─ ticket.validated.json
├─ draw.completed.json
└─ ...
```

- Nom du fichier = `{eventType}.json` (snake_case avec point comme séparateur)
- Si aucun fichier n'existe → `loadDecisionSetForEvent()` retourne `null` → l'événement est ignoré silencieusement

---

## Exemple complet

```json
{
  "name": "ticket.created",
  "decisions": [
    {
      "conditions": {
        "all": [
          { "fact": "eventType", "operator": "equal", "value": "ticket.created" },
          {
            "fact": "tenant",
            "operator": "equal",
            "value": true,
            "path": "$.features.sms_ticket_created"
          },
          { "fact": "user", "operator": "equal", "value": "fr", "path": "$.locale" }
        ]
      },
      "event": {
        "type": "send_notification",
        "params": {
          "channel": "SMS",
          "provider": "bird",
          "templateId": "sms_ticket_created_fr"
        }
      }
    }
  ]
}
```

---

## API interne

```typescript
// src/core/rule-engine.ts

// Charge le DecisionSet depuis rules/{eventType}.json
// Retourne null si le fichier n'existe pas
export async function loadDecisionSetForEvent(eventType: string): Promise<DecisionSet | null>;

// Évalue le DecisionSet contre des facts
// Retourne les events déclenchés
export async function evaluateDecisionSet(decisionSet: DecisionSet, facts: any): Promise<any[]>;
```

---

## Opérateurs json-rules-engine disponibles

| Opérateur                                    | Exemple                                          |
| -------------------------------------------- | ------------------------------------------------ |
| `equal`                                      | `"operator": "equal", "value": "ticket.created"` |
| `notEqual`                                   |                                                  |
| `greaterThan` / `lessThan`                   | pour les nombres                                 |
| `greaterThanInclusive` / `lessThanInclusive` |                                                  |
| `in` / `notIn`                               | `"value": ["fr", "en"]`                          |
| `contains` / `doesNotContain`                | pour les tableaux                                |

---

## Path dans les facts

Pour accéder à des propriétés imbriquées, utiliser `path` (JSONPath) :

```json
{ "fact": "tenant", "operator": "equal", "value": true, "path": "$.features.sms_ticket_created" }
```

---

## Endpoints règles

```
POST /api/rules/evaluate
Body: { decisionSet: DecisionSet, facts: object }
Response: { events: DecisionEvent[] }

POST /api/rules/validate
Body: { decisionSet: DecisionSet }
Response: { valid: boolean, errors?: string[] }
```

---

## Ajouter un nouveau DecisionSet

1. Créer `rules/{eventType}.json` avec la structure `DecisionSet`
2. Définir chaque `decision` : `conditions` (all/any/not) + `event`
3. L'événement `type` doit correspondre à une action gérée par `communication-handler.ts` (ex. `send_notification`)
4. Tester via `POST /api/rules/evaluate` avec des facts de test
