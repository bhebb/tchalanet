# Tchalanet Infra Context Pack

Petit pack à déposer dans le contexte de `tchalanet-infra` pour guider Claude/Codex/agents.

Contenu :

- `00-infra-charter.md` — charte infra officielle v0.
- `01-infra-decisions.md` — décisions actées à respecter.
- `02-claude-context.md` — contexte court à donner à Claude.
- `03-scan-checklist.md` — checklist de scan avant proposal.

Usage conseillé :

```bash
# Depuis la racine de tchalanet-infra
mkdir -p docs/context
cp 00-infra-charter.md docs/00-infra-charter.md
cp 01-infra-decisions.md docs/context/01-infra-decisions.md
cp 02-claude-context.md docs/context/02-claude-context.md
cp 03-scan-checklist.md docs/context/03-scan-checklist.md
```

Puis demander à Claude :

```text
Lis d'abord docs/00-infra-charter.md et docs/context/*.md. Respecte ces décisions avant toute proposition ou modification.
```
