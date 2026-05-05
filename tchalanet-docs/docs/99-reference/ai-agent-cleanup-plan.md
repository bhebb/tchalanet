# AI-Agent Cleanup And Archival Plan

No AI-agent file should be deleted directly. Archive first, then delete only in a follow-up reviewed change.

| Action                          | Classification       | Meaning                                            | Count |
| ------------------------------- | -------------------- | -------------------------------------------------- | ----: |
| keep                            | `CANONICAL`          | Canonical router or source of truth.               |     7 |
| keep short                      | `TOOL_ROUTER`        | Tool-specific entrypoint; link to canonical rules. |    89 |
| keep near component             | `COMPONENT_SPECIFIC` | Component-owned detail.                            |    10 |
| convert or archive after review | `DUPLICATE`          | Likely duplicate content.                          |     0 |
| archive after review            | `OBSOLETE`           | Stale instruction candidate.                       |     0 |
| keep archived                   | `ARCHIVE`            | Already archived.                                  |     0 |
| review                          | `UNKNOWN`            | Ownership/action unclear.                          |     2 |

## Review Queue

| Path                                           | Classification | Recommended action                               |
| ---------------------------------------------- | -------------- | ------------------------------------------------ |
| `.claude/settings.local.json`                  | UNKNOWN        | Review ownership before archiving or converting. |
| `tchalanet-server/.claude/settings.local.json` | UNKNOWN        | Review ownership before archiving or converting. |
