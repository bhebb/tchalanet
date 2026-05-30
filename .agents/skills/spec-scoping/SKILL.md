# spec-scoping

## Use when

Turning a vague idea into an implementable spec. This is the skill behind `/spec`. No code is written.

Also use when a spec is **in draft and needs deeper analysis** — see "Draft deepening" below.

## Load

1. `AGENTS.md` (root router).
2. `.agents/skills/openspec-workflow/SKILL.md`.
3. Only the routers/docs needed to identify impact — do not load implementations.

## Do

- Identify impacted projects/modules.
- Identify the source docs to read or update.
- Break the work into bounded tasks (one slice each).
- Mark anything unstable as an assumption.
- Recommend whether an OpenSpec change is required (it usually is for new capability/refactor).

## Do not

- Write or edit code.
- Load whole projects to scope — read routers and named docs only.
- Decide business rules — surface open questions instead.

## Output

```
1. Need (summary)
2. Impacted projects / modules
3. Relevant source docs
4. Task breakdown (one slice each)
5. Risks
6. Open questions
7. Assumptions
```

## Draft deepening

Quand une spec est **en draft** et que l'analyse doit être approfondie :

1. **Lire la spec** : `openspec/changes/<change>/proposal.md` + `tasks.md`.
2. **Identifier les zones floues** : open questions, assumptions marquées, sections vides.
3. **Recherche ciblée** : lire uniquement les docs sources nommés dans la spec (pas de scan global). Exemples : un `DOMAIN_*.md`, un `flow.md`, un contrat API.
4. **Mettre à jour `proposal.md`** : compléter les sections floues, déplacer assumptions → décisions si clarifiées, ajouter open questions nouvelles.
5. **Mettre à jour `tasks.md`** : affiner le découpage si la recherche change la granularité.
6. **Ne pas coder** — si la recherche révèle qu'une implémentation est nécessaire pour valider, le noter comme open question.

Output : `proposal.md` mis à jour avec sections `## Deepened — <date>` pour tracer ce qui a changé.

## OpenSpec pipeline (obligatoire)

Chaque change OpenSpec suit ce pipeline — ne pas sauter d'étape :

```
proposal.md créé
    ↓
tasks.md créé (tâches checkables, une par slice)
    ↓
[par tâche] implémentation → valider → cocher dans tasks.md
    ↓
[par tâche] mise à jour docs concernés (voir "Doc update" dans scoped-task)
    ↓
Toutes tâches cochées → archiver via opsx:archive
```

**Checkpoint de session** : au début de chaque session sur un change existant, lire `tasks.md` en premier pour savoir où on en est. Ne jamais repartir de zéro.

## Mobile output (Slack)

Si la session vient d'un terminal mobile (Termius/SSH) ou si le résultat dépasse ~50 lignes :
- Envoyer le résultat complet en markdown dans `#tchalanet-agents` (`C0B76AV9WAW`) via Slack MCP.
- Format : titre `📋 Spec — <sujet>` + les 7 sections en markdown.
- Mentionner dans le terminal : « Spec envoyée sur #tchalanet-agents ».
