# Workflow mobile — dev depuis le téléphone

> Référence rapide. Stack : iPhone + Tailscale + Termius + tmux + Claude Code.

## Stack utilisée

| Outil | Rôle | Gratuit ? |
|---|---|---|
| **Tailscale** | VPN mesh — connecte le tel au Mac sans expo SSH publique | ✅ |
| **Termius** (iOS) | Client SSH | ✅ (basique) |
| **tmux** | Sessions terminal persistantes sur le Mac | ✅ |
| **Claude Code** | Agent AI dans le terminal | selon plan |
| **Slack #tchalanet-agents** | Réception des outputs longs (spec, ready-check, handoff) | ✅ |

## Connexion (checklist rapide)

```
1. Tailscale ON sur l'iPhone
2. Termius → hôte mac-tchalanet → Connect
3. tmux attach -t claude   (ou tmux new -s claude si premiere fois)
4. claude                  (si la session Claude n'est pas déjà active)
```

Si `tmux attach` dit "no session" → `tmux new -s claude && claude`.

## Clavier Termius — raccourcis importants

| Action | Comment |
|---|---|
| Détacher tmux (laisser tourner) | Barre spéciale → `Ctrl` + `B`, relâcher, puis `D` |
| Remonter dans le terminal | `Ctrl+B` puis `[` → flèches/PageUp → `Q` pour sortir |
| Nouvelle fenêtre tmux | `Ctrl+B` puis `C` |
| Lister les fenêtres | `Ctrl+B` puis `W` |

> La **barre de touches spéciales** de Termius (au-dessus du clavier) contient `Ctrl`, `Tab`, `Esc`, flèches.

## Outputs longs → Slack

Les résultats trop longs à lire dans Termius sont envoyés automatiquement dans `#tchalanet-agents` :

| Commande | Envoi Slack auto |
|---|---|
| `/spec` | ✅ si > 50 lignes ou session mobile |
| `/ready-check` | ✅ si > 30 lignes |
| `handoff` | ✅ toujours |
| `/backend-task` `/web-task` | ❌ (résultat court) |

## Précautions

| Risque | Précaution |
|---|---|
| Session qui coupe (réseau mobile) | tmux protège côté Mac, Termius SSH se reconnecte — rien perdu |
| Mac qui s'endort | Lancer `caffeinate -dims` sur le Mac avant de partir, Mac branché |
| Token/secret qui apparaît dans le terminal | `HISTCONTROL=ignorespace` + ne jamais coller de token directement |
| SSH par mot de passe (actuel) | À migrer vers clé SSH (Keychain Termius → copier clé pub → `~/.ssh/authorized_keys`) |
| PAT GitHub en clair | ⚠️ Régénérer sur GitHub → Settings → Developer settings → Fine-grained tokens |

## À faire (sécurité, non encore fait)

- [ ] SSH par clé dans Termius (remplacer auth par mot de passe)
- [ ] Régénérer le PAT GitHub exposé lors du test MCP
- [ ] Activer `HISTCONTROL=ignorespace` dans `~/.zshrc` ou `~/.bashrc` du Mac

## Routine type depuis le tel

```
# Connexion
Tailscale ON → Termius → mac-tchalanet

# Reprendre le travail
tmux attach -t claude
/spec ou /backend-task ou /web-task ...

# Résultats → lire sur Slack #tchalanet-agents si long

# Fin de session
handoff → Ctrl+B D   (détacher, laisser tourner)
```

## MCP actifs (2026-05-30)

| MCP | Usage | Review |
|---|---|---|
| GitHub (PAT) | PR, issues | 2026-06-13 |
| Slack (claude.ai) | #tchalanet-agents | 2026-06-13 |

Voir `.agents/mcp-activations.md` pour le log complet.
