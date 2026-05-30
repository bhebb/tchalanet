# scoped-task

## Use when

Implementing or reviewing a bounded task inside a single project (backend, web, mobile, edge, infra, docs). This is the default skill behind `/backend-task`, `/web-task`, `/mobile-task`, and similar.

This is a **workflow** skill. It carries no business rules — those live in the project's `docs/` and `openspec/`.

## Load

1. `AGENTS.md` (root router).
2. The target project's `AGENTS.md`.
3. At most one near-code doc the task points to.
4. The files being edited or reviewed.

Target: <500 lines outside source code.

## Do

- Lock scope to the declared project. State the slice before editing.
- Use targeted reads (path + line range) first.
- Follow the project's conventions by pointer (read its `docs/`/`openspec/`), do not re-derive them here.
- Run the project's validation command before declaring done.
- Reference `.agents/skills/ai-safety/SKILL.md` for sensitive actions.
- **Après chaque tâche terminée** : mettre à jour les docs concernés (voir "Doc update" ci-dessous).
- **Cocher la tâche** dans `tasks.md` de l'OpenSpec change associé avant de passer à la suivante.
- End with the `.agents/skills/handoff/SKILL.md` output format.

## Doc update (obligatoire après chaque change)

Après avoir terminé une implémentation, identifier et mettre à jour :

| Type de doc | Quand | Où |
|---|---|---|
| `DOMAIN_*.md` / `CATALOG_*.md` / `PLATFORM_*.md` | si le comportement du composant change | près du composant dans `docs/` ou `src/` |
| Convention (`docs/conventions/`) | si une règle de fonctionnement change | le fichier de convention concerné |
| Flow (`docs/flows/` ou `openspec/`) | si le flux (appel, auth, paiement…) est modifié | le doc de flow impacté |
| `openspec/changes/<change>/tasks.md` | toujours | cocher `[x]` la tâche accomplie |

**Règle** : si aucun de ces docs n'est impacté, le noter explicitement dans le handoff (`Docs updated: none — changes are internal only`). Ne jamais laisser la question ouverte.

## Do not

- Touch files outside the declared slice.
- Preload sibling project routers.
- Run a global scan (`grep -R`, `find /`, `tree` at root) without an explicit reason.
- Inline business/domain rules — point to the source.
- Bump versions/deps, touch secrets/migrations/auth/RLS/infra without explicit approval.

## Output

```
Slice: <project>
Files inspected: <paths>
Files changed: <paths>
Tests run: <command + result>
Risks: <list or none>
Next step / handoff: <one concrete action>
```
