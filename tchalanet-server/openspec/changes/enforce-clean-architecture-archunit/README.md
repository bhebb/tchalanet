# enforce-clean-architecture-archunit — package OpenSpec + docs

Ce zip contient une proposition OpenSpec pour :

1. renforcer la clean architecture via ArchUnit ;
2. clarifier `domain/service` vs `application/service` ;
3. standardiser `port.out` et interdire `port.in` par défaut ;
4. clarifier les communications inter-domaines ;
5. renommer les méthodes des bus :
   - `CommandBus.execute(command)`
   - `QueryBus.ask(query)`
   - `Handler.handle(message)` reste réservé aux handlers ;
6. mettre à jour la documentation backend.

## Comment l’utiliser

Depuis la racine du monorepo :

```bash
unzip enforce-clean-architecture-archunit.zip -d /tmp/tchalanet-arch
cp -R /tmp/tchalanet-arch/openspec/changes/enforce-clean-architecture-archunit openspec/changes/
cp /tmp/tchalanet-arch/tchalanet-server/docs/conventions/*.md tchalanet-server/docs/conventions/
```

Ensuite :

```bash
openspec validate enforce-clean-architecture-archunit --strict
```

Puis appliquer progressivement les tâches dans `openspec/changes/enforce-clean-architecture-archunit/tasks.md`.

## Note importante

Les fichiers Java inclus sont des squelettes / templates de migration. Ils doivent être adaptés à l’état réel du repo avant commit.
