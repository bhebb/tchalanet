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
<!-- caffeinate -d -->
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

## Serveur tchalanet.lan (Manjaro Linux)

> Serveur local LAN — fait tourner l'API backend + edge service pour le dev web sans charger le Mac.

### Quand l'utiliser

| Contexte | Sans tchalanet.lan | Avec tchalanet.lan |
|---|---|---|
| Dev `tchalanet-web` | démarrer backend + edge sur le Mac | pointer le web vers le serveur |
| Test API isolé | tout local | utiliser le serveur |
| Mac faible en ressources | Java + Node + Angular = lourd | soulager le Mac |

**Limite** : LAN local uniquement — pas accessible depuis l'extérieur ni via Tailscale (pas encore configuré).

### Connexion SSH

```bash
# Depuis le Mac ou Termius (sur le même réseau Wi-Fi)
ssh <user>@tchalanet.lan
# ou par IP si mDNS ne résout pas :
ssh <user>@192.168.x.x
```

Dans Termius : ajouter un hôte `tchalanet-lan` avec Hostname `tchalanet.lan`, Port `22`, Username `<ton-user-manjaro>`.

### Services qui tournent

| Service | Projet | Port défaut |
|---|---|---|
| API Spring Boot | `tchalanet-server` | `8080` |
| Edge service | `tchalanet-edge-service` | `3000` (à confirmer) |

### Démarrer / arrêter les services

```bash
# Se connecter
ssh <user>@tchalanet.lan
tmux attach -t backend   # ou tmux new -s backend

# Démarrer l'API (depuis tchalanet-server/)
./mvnw spring-boot:run

# Démarrer l'edge (depuis tchalanet-edge-service/)
npm run dev

# Détacher sans arrêter : Ctrl+B D
```

### Configurer le web pour pointer vers tchalanet.lan

Dans `tchalanet-web`, le fichier d'env local à modifier :

```bash
# tchalanet-web/.env.local  (ou proxy config Nx)
API_URL=http://tchalanet.lan:8080
EDGE_URL=http://tchalanet.lan:3000
```

> Vérifier dans `tchalanet-web/AGENTS.md` ou `docs/` la convention exacte pour l'URL d'API en dev local.

### Précautions spécifiques

| Risque | Précaution |
|---|---|
| Serveur éteint/suspendu | Vérifier que Manjaro ne se suspend pas (`systemd-sleep` ou réglages énergie) |
| LAN uniquement | Pas accessible depuis l'extérieur — revenir sur le Mac si hors réseau |
| Migrations DB | Ne jamais lancer une migration sur le serveur sans sauvegarde |
| Tailscale à ajouter | Quand dispo : ajouter `tchalanet.lan` dans Tailscale → accessible depuis le tel partout |

### TODO tchalanet.lan

- [ ] Confirmer ports réels (edge-service notamment)
- [ ] Ajouter Tailscale sur le serveur Manjaro (accès distant depuis le tel)
- [ ] SSH par clé (même principe que le Mac)
- [ ] Ajouter hôte `tchalanet-lan` dans Termius

## MCP actifs (2026-05-30)

| MCP | Usage | Review |
|---|---|---|
| GitHub (PAT) | PR, issues | 2026-06-13 |
| Slack (claude.ai) | #tchalanet-agents | 2026-06-13 |

Voir `.agents/mcp-activations.md` pour le log complet.
