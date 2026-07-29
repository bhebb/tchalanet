# OpenSpec Change — Console Sidenav Simplification V1

## Status

**Livré et déployé** — 2026-07-29. [PR #437](https://github.com/bhebb/tchalanet/pull/437) (split
backend + interaction ligne/chevron initiale) et [PR #438](https://github.com/bhebb/tchalanet/pull/438)
(deux corrections trouvées en usage réel après #437, voir ci-dessous) mergées dans `main`, vérifiées
en direct sur `test.tchalanet.com`.

Deux ajustements post-merge, décidés sur retour utilisateur réel plutôt qu'anticipés à la
conception — documentés en détail dans `specs/web-console-sidenav/spec.md` :

1. **La scission libellé/chevron ne s'applique qu'au desktop.** Portée sur mobile dans un premier
   temps (cohérence avec desktop), puis retirée le jour même : le drawer mobile se referme
   entièrement après navigation, contrairement à l'accordéon desktop qui reste ouvert et se
   rouvre sur le groupe actif — sans lui, le petit chevron devenait la seule façon de jamais
   découvrir les pages sœurs, ce que personne ne trouvait en usage réel.
2. **Le panneau mobile ne filtre plus l'entrée d'atterrissage.** Le titre de panneau cliquable
   (introduit pour compenser le filtrage) ressemblait à du texte, pas à un lien — invisible comme
   affordance. Chaque enfant, atterrissage compris, est maintenant une ligne ordinaire, comme la
   sidebar desktop l'a toujours fait.
3. **Surbrillance desktop corrigée** : un groupe et son enfant actif ne partagent plus le même
   aplat de couleur plein (voir spec pour le détail du bug de spécificité CSS rencontré).

## Why

Les administrateurs du back-office sont peu à l'aise avec la navigation web, même formés. Chaque
clic évitable, chaque libellé ambigu, compte.

### Ce qui existe vraiment aujourd'hui (vérifié en direct, pas supposé)

`web-console-drawer-two-levels-v1` (PR #435) a câblé une absorption partielle : quand un groupe a
un `path` de parent identique à un enfant `activeMatch: 'exact'`, cet enfant disparaît de la liste
du panneau. Mais en testant réellement sur mobile (resize 390×844 sur staging authentifié) :

- Taper « Jwèt » à la racine **ouvre le panneau** (n'atterrit pas directement sur la page) — le
  panneau montre alors « Jwèt » comme titre et un seul enfant restant, « Matriks jwèt pa règ tiraj »
  (Apèsi est bien absent de la liste). Taper ensuite le **titre** du panneau navigue vers
  `/app/admin/games`, dont la page est déjà intitulée « Jwèt disponib » — mais rien dans le panneau
  n'indique visuellement que ce titre est cliquable. Résultat réel : **2 taps, dont un sur un texte
  qui ne ressemble pas à un lien.**
- Sur desktop, aucune absorption : déplier « Jwèt » montre encore « Apèsi » comme ligne à part
  entière (2 clics visibles, mais au moins l'un des deux est un vrai lien affiché).

Donc l'absorption actuelle a fait disparaître le mot « Apèsi », pas le clic superflu — et sur
mobile, elle l'a remplacé par un geste moins découvrable. Ce n'est pas encore ce qui est demandé.

### Ce qui est visé : la ligne elle-même navigue

- **Clic sur le libellé/icône de la ligne parent → navigation directe** vers sa page principale.
- **Clic sur le chevron → seulement replier/déplier** les sous-pages, sans naviguer.
- Le chevron n'apparaît que si le groupe a effectivement plusieurs sous-pages utiles au-delà de sa
  page principale.

C'est un vrai changement d'interaction (pas un simple portage mobile→desktop comme supposé dans une
version antérieure de ce document), applicable identiquement aux deux ruptures d'écran.

### Deux groupes déjà prêts, cinq à préparer

`games` et `limits` ont déjà un `path` de parent égal à celui de leur enfant d'atterrissage — la
règle ci-dessus peut s'y appliquer directement. Cinq autres groupes n'ont **pas de `path` sur leur
item parent** dans `private_shell_tenantadmin.json` : `sellers` (Tèminal POS), `draws` (Tiraj),
`reports` (Rapò), `tickets` (Tikè), `company` (Antrepriz mwen). Leur en-tête ne mène nulle part par
lui-même tant qu'on ne le leur donne pas.

### Deux problèmes réels trouvés en vérifiant les traductions (`surface-admin.json`, HT)

Ni signalés dans les conseils d'origine, ni visibles sans comparer le JSON aux clés i18n :

- **`reports-sales` s'affiche « Tikè »**, pas « Vant » — un rapport de ventes porte le même mot que
  le module Tikè (billets) situé juste au-dessus dans le même menu. Whichever la page par défaut
  choisie pour `reports`, ce libellé reste trompeur tant qu'il n'est pas corrigé.
- **« Règ tiraj » est utilisé deux fois** pour deux pages différentes : `draws-channels`
  (`/app/admin/draws/channels`) et `limits-draw` (`/app/admin/limits/draw`). Un utilisateur qui
  cherche l'un après avoir vu l'autre atterrit au mauvais endroit.

Les deux vont exactement à l'encontre de l'objectif de ce change et sont donc traités ici, pas
reportés.

## Decisions (locked)

1. **La ligne parent navigue, le chevron replie/déplie** — pour tout groupe qui a (ou reçoit) une
   destination propre, sur mobile et desktop identiquement.
2. **`sellers` et `draws` reçoivent un `path` de parent** égal à la route de leur enfant
   d'atterrissage actuel (`sellers-list` → `/app/admin/seller-terminals`, `draws-all` →
   `/app/admin/draws`) — ces enfants sont déjà `activeMatch: 'exact'`, aucun renommage nécessaire,
   risque identique à `games`/`limits`.
3. **`reports` et `tickets` reçoivent un `path` de parent**, et leur enfant `*-overview` (« Apèsi »)
   disparaît du menu — la route `/app/admin/reports/overview` / `/app/admin/tickets/overview` reste
   en place (aucune route supprimée), simplement plus liée depuis le menu :
   - `reports` → `/app/admin/reports/daily` (« Jounen »), le rapport le plus consulté au quotidien.
   - `tickets` → `/app/admin/tickets` (« Tout tikè », déjà la page `tickets-list`).
4. **`company` reçoit un `path` de parent** = `/app/admin/business-profile` (`company-identity`,
   déjà la page « Idantite »).
5. **Corriger les deux problèmes de libellé trouvés** : `reports-sales` → un mot qui dit « ventes »,
   pas « Tikè » ; renommer l'un des deux « Règ tiraj » pour qu'ils ne soient plus identiques.
6. **Pas de renommage pour `limits-system`** : son libellé réel est déjà « Definisyon & similasyon »,
   pas « Sistèm » comme supposé — rien à faire ici, la préoccupation d'origine ne s'applique plus.
7. **Pas de nouvelle entrée pour le barème des jeux** : la page « Jwèt disponib » a déjà un bouton
   « Gade barèm yo » — le tarif est une action dans la page, pas un besoin de sous-lien séparé.

## Explicitement hors périmètre (nécessitent une décision produit séparée, pas une simplification de nav)

- **« PIN & blokaj » pour Tèminal POS** : n'existe pas aujourd'hui, ni comme page ni comme action.
  Créer cette fonctionnalité est un chantier produit à part, pas un renommage/déplacement de nav.
- **« Itilizatè » sous Antrepriz mwen** : n'existe pas dans le fragment backend actuel (ses 6
  enfants réels sont Idantite, Aparans, Paramèt, Sant notifikasyon, Modèl paj, Sipò). Si une page de
  gestion des utilisateurs tenant-admin est voulue, c'est une nouvelle capacité à spécifier
  séparément, pas une réorganisation de l'existant.

## Impact

- Console admin (`TENANT_ADMIN_NAVIGATION` + `private_shell_tenantadmin.json`) **et** console
  platform (`PLATFORM_NAVIGATION` + `private_shell_superadmin.json`) — périmètre élargi après coup :
  `tenants` et `operations` avaient déjà, côté modèle statique, une destination correspondant à un
  enfant `activeMatch: 'exact'`, mais le fragment backend ne l'avait jamais eue — même écart que les
  cinq groupes admin, corrigé pareil. `audit` et `archives` gagnent une destination pour la même
  raison (chacun un seul enfant `Apèsi`/« Odit fonctionnel » candidat évident). `dashboard`, `access`,
  `references`, `support-and-content` et `tchala` restent sans destination — aucun n'a un enfant par
  défaut évident (`dashboard` en a même deux à égalité, santé technique vs commercial).
- Touche le contrat backend (`private_shell_tenantadmin.json` pour 5 groupes,
  `private_shell_superadmin.json` pour 4 groupes), et deux composants frontend partagés
  (`tch-drawer-nav`, `tch-sidebar-nav`) pour le nouveau modèle clic-ligne/clic-chevron — un seul
  changement de composant profite aux deux consoles puisqu'elles consomment le même rendu.
- Aucune route ni permission supprimée — uniquement la navigation et deux libellés admin.

## Non-goals

- **Le bouton de connexion introuvable sur le portail public** reste un sujet séparé (shell public,
  pas console) — non traité ici.
- Pas de refonte du split opérations/configuration déjà livré en PR #435 (ni son extension à
  `PLATFORM_NAVIGATION`, qui n'a toujours qu'une seule section).
- Pas de destination choisie pour `dashboard`/`access`/`references`/`support-and-content`/`tchala`
  côté platform — même statut que `reports`/`company` l'étaient côté admin avant que ce change leur
  en choisisse une : une décision produit, à trancher séparément si voulue.
- Pas de regroupement des rapports en onglets dans une même page — resterait une évolution de
  `reports`, pas de la navigation ; à évaluer séparément si voulu.
