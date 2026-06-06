# Tchalanet Design System

> Status: normative  
> Scope: Web, Mobile, POS, Landing, Tenant Admin  
> Version: WEB_UI_FOUNDATION_V1

Ce dossier définit les règles UI normatives pour Tchalanet.

## Documents

1. `01-design-principles.md`
2. `02-design-tokens.md`
3. `03-components.md`
4. `04-pos-home-reference.md`
5. `05-surfaces-web-mobile-pos.md`
6. `06-accessibility.md`
7. `07-implementation-mapping.md`

## Implémentations techniques

Implémentation active :

```text
tchalanet-web/libs/ui/theme/
tchalanet-web/libs/ui/styles/
tchalanet-web/libs/ui/components/
```

Le Web est la référence active de cette version. L’alignement Mobile puis POS sera réalisé dans des
changements dédiés, propriétaires de leurs implémentations Flutter. Les règles et tokens décrits ici
ne signifient pas que ces surfaces sont déjà migrées.

## Règle principale

Les exports Figma/Stitch sont des références visuelles, pas des contrats techniques directs.  
Toutes les valeurs doivent passer par les tokens Tchalanet avant d’être codées.
