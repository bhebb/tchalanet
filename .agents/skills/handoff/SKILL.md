# handoff

## Use when

You finish a task, hit the 3-attempt stop, or need to hand work to the next session before `/clear`.

## Load

- This file.
- The current task's touched files (already in context).

## Do

- Produce a compact handoff. Keep it short — it is a pointer, not a transcript.
- Name files with repo-relative paths.
- State the next concrete step so a fresh session can resume cold.

## Do not

- Paste large diffs or full file contents.
- Restate durable rules — point to `docs/` or the project `AGENTS.md`.

## Output

```
Slice: <project>
Files inspected: <paths>
Files changed: <paths>
Tests run: <command + result>
Risks: <list or none>
Next step: <one concrete action>
```

## Mobile output (Slack)

Toujours envoyer le handoff dans `#tchalanet-agents` (`C0B76AV9WAW`) — c'est le cas d'usage principal.
Format : titre `📋 Handoff — <slice> — <date>` + les 6 sections.
