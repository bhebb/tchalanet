# Terminal Reorg + Device Proof Security — OpenSpec Pack

Pack de cadrage pour challenger Claude/Copilot avant implémentation.

Objectifs :

1. nettoyer `core.terminal` : lifecycle, assignment, metadata, operational-controls, activation/binding, runtime ;
2. renforcer le binding POS/mobile : challenge, clé publique POS, signature de requête, anti-replay ;
3. introduire les grants signés backend -> POS pour offline ;
4. garder les responsabilités propres : `core.terminal` possède le binding device, `platform.keymanagement` signe côté serveur, `core.offlinesync` crée les grants métier.

Ordre conseillé :

1. `tasks/00-cleanup-terminal-controllers.md`
2. `tasks/01-terminal-challenge-binding.md`
3. `tasks/02-device-proof-signatures.md`
4. `tasks/03-signed-offline-grants.md`
5. `tasks/04-tests-matrix.md`
