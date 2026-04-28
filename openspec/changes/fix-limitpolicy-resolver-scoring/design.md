## Context

`LimitResolver.score()` retourne un entier positif pour chaque sous-type de `LimitTarget`. Plus le score est élevé, plus l'assignment est considéré comme "spécifique" et remplace les autres pour la même `RuleKey`.

Hiérarchie actuelle :

| LimitTarget         | Score |
| ------------------- | ----- |
| `TenantTarget`      | 10    |
| `OutletTarget`      | 50    |
| `AgentTarget`       | 60    |
| `TerminalTarget`    | 70    |
| `DrawChannelTarget` | 40    |

Lecture du code source (`LimitResolver.java` l. 94–102) :

```java
private int score(LimitTarget target, LimitContext ctx) {
  // higher = more specific
  return switch (target) {
    case LimitTarget.TenantTarget ignored -> 10;
    case LimitTarget.OutletTarget t -> ... ? 50 : -1;
    case LimitTarget.AgentTarget t -> ... ? 60 : -1;
    case LimitTarget.TerminalTarget t -> ... ? 70 : -1;
    case LimitTarget.DrawChannelTarget t -> ... ? 40 : -1;
  };
}
```

**Questions de sémantique :**

1. **Terminal (70) > Agent (60)** : un Terminal est un équipement physique ; un Agent est la personne qui l'utilise. Dans un contexte POS, une règle par Agent est souvent plus personnalisée qu'une règle par Terminal. L'intention était-elle Terminal > Agent, ou le contraire ?

2. **DrawChannel (40) < Outlet (50)** : un DrawChannel est plus transversal qu'un Outlet (point de vente). L'ordre semble correct.

3. **Tenant (10)** : le niveau le plus global — correct.

**Comparaison avec d'autres systèmes** : les systèmes de règles métier classiques appliquent la spécificité en fonction de la granularité de l'entité. Dans l'ordre de granularité POS habituel : Tenant < DrawChannel < Outlet < Agent < Terminal (le Terminal est le plus fin car c'est le device exact, une session précise).

Le scoring actuel (Terminal > Agent > Outlet > DrawChannel > Tenant) est **cohérent avec cette lecture de granularité** — Terminal est le plus spécifique car c'est le contexte d'exécution le plus précis. L'anomalie de l'audit est plus une **question documentaire** qu'un bug réel.

## Goals / Non-Goals

**Goals:**

- Documenter explicitement la sémantique du scoring dans le code (`LimitResolver`)
- Ajouter des tests unitaires couvrant les conflits de priorité entre cibles
- Valider ou corriger l'ordre si la product décision va à l'encontre de l'implémentation actuelle

**Non-Goals:**

- Modifier l'algorithme de résolution (beyond scoring)
- Ajouter de nouveaux types de `LimitTarget`

## Decisions

### D1 — Sémantique du scoring : Terminal > Agent confirmé

La hiérarchie de spécificité POS est : Tenant < DrawChannel < Outlet < Agent < Terminal.

Un Terminal identifie un équipement précis (ex. terminal POS Motorola #42 dans un outlet). C'est le contexte le plus spécifique possible — une règle définie pour ce terminal précis doit l'emporter sur une règle pour l'agent qui l'utilise.

**Décision** : le scoring actuel est intentionnel. Documenter et ajouter des tests.

Si un stakeholder conteste cette décision → créer un ADR.

### D2 — Tests de préséance obligatoires

Ajouter des tests couvrant :

- Terminal vs Agent : Terminal gagne
- Agent vs Outlet : Agent gagne
- Outlet vs DrawChannel : Outlet gagne
- DrawChannel vs Tenant : DrawChannel gagne
- Conflit avec cible non-applicable (-1) : cible valide gagne

## Risks / Trade-offs

- **[Risque] Décision produit non documentée** : si le scoring est effectivement incorrect (Agent devrait primer sur Terminal dans certains contextes métier), le corriger après déploiement peut impacter des tenants en production. → Communication proactive avec les parties prenantes avant implémentation.
- **[Trade-off] Scoring simple entier vs hiérarchie multicritère** : le scoring à entier unique ne gère pas les cas de conflit complexes (ex. Agent _et_ Terminal dans le même contexte). → Acceptable pour le MVP ; noter comme limitation dans le code.
