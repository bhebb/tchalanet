# TODO — Corriger le topbar mobile du private shell

## Contexte

Sur mobile, le `PrivateShell` affiche actuellement une deuxième ligne dans le topbar avec :

```text
FR · dark mode · avatar
```

Le rendu donne une impression cassée : les actions flottent sous le logo au lieu d’être alignées dans une vraie barre d’action. Pour V0, le topbar mobile doit rester simple, lisible et stable.

## Décision UX

### Desktop

Garder le layout complet :

```text
Logo Tchalanet        Plateforme        FR · dark mode · SA
```

### Mobile / tablet compact

Utiliser une seule ligne :

```text
☰  Tchalanet                         SA
```

Sur mobile, masquer :

```text
- le titre de surface “Plateforme”
- le sélecteur de langue FR
- le toggle dark mode
```

Ces actions iront plus tard dans le menu utilisateur / avatar.

## Scope V0

Cette tâche concerne uniquement le rendu responsive du topbar privé.

Ne pas refondre :

```text
- le sidenav
- les routes
- la logique auth
- le thème global
- les menus utilisateur avancés
```

## Fichiers probables

À adapter selon le projet réel :

```text
PrivateShellComponent
TchTopBar / private topbar component
private-shell.component.scss
private-topbar.component.scss
```

## Règles de layout mobile

À partir de `max-width: 768px` :

```text
- topbar sur une seule ligne
- hauteur stable
- menu burger à gauche
- brand Tchalanet au centre/gauche
- avatar à droite
- aucune action ne doit passer sur une deuxième ligne
```

## SCSS cible

Exemple de direction :

```scss
@media (max-width: 768px) {
  .private-topbar {
    height: 4.5rem;
    padding: 0 1rem;
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: 0.75rem;
  }

  .private-topbar__menu-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .private-topbar__brand {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    min-width: 0;
  }

  .private-topbar__brand-label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .private-topbar__surface-title,
  .private-topbar__language,
  .private-topbar__theme-toggle {
    display: none;
  }

  .private-topbar__actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 0.5rem;
    min-width: 0;
  }
}
```

Adapter les noms de classes aux classes existantes.

## Variante acceptable

Si on veut absolument garder la langue visible en mobile, accepter seulement :

```text
☰  Tchalanet                      FR  SA
```

Dans ce cas, masquer uniquement :

```text
- Plateforme
- dark mode
```

Mais la recommandation V0 reste :

```text
☰  Tchalanet                         SA
```

## Menu avatar futur

Plus tard, le menu avatar pourra contenir :

```text
- Profil
- Langue : FR / HT / EN
- Mode sombre
- Déconnexion
```

Ce n’est pas obligatoire dans cette tâche.

## Critères d’acceptation

- En mobile, le topbar tient sur une seule ligne.
- Il n’y a plus de deuxième ligne avec `FR`, lune et avatar.
- Le menu burger reste visible à gauche.
- Le logo/nom Tchalanet reste visible.
- L’avatar utilisateur reste visible à droite.
- Le titre `Plateforme` est masqué en mobile.
- Le sélecteur langue et le toggle dark mode sont masqués en mobile.
- Le rendu desktop reste inchangé.
- Aucun élément du topbar ne déborde ou ne wrap entre `360px` et `768px`.
- Les pages onboarding, liste tenants et détail tenant restent utilisables en mobile.

## Priorité

Priorité moyenne.

À faire après la validation desktop des écrans :

```text
1. Onboarding tenant
2. Liste tenants
3. Détail tenant
4. Responsive topbar mobile
```
