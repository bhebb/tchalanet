# spec-scoping

## Use when

Turning a vague idea into an implementable spec. This is the skill behind `/spec`. No code is written.

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

## Mobile output (Slack)

Si la session vient d'un terminal mobile (Termius/SSH) ou si le résultat dépasse ~50 lignes :
- Envoyer le résultat complet en markdown dans `#tchalanet-agents` (`C0B76AV9WAW`) via Slack MCP.
- Format : titre `📋 Spec — <sujet>` + les 7 sections en markdown.
- Mentionner dans le terminal : « Spec envoyée sur #tchalanet-agents ».
