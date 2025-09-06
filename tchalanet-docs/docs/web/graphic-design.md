# 🎨 Charte graphique Tchalanet

## 1. Identité visuelle

-   **Logo** :
    -   Version principale : "Tchalanet" en bleu #0052CC avec symbole
        boule de loto stylisée (gradient bleu → blanc).\
    -   Versions secondaires : monochrome (noir, blanc) pour fonds
        spéciaux.\
    -   Utiliser un espace de respiration équivalent à la hauteur du "T"
        autour du logo.
-   **Slogan** (optionnel) : *"La loterie connectée, simple et
    sécurisée"*.
    -   Police : la même que le corps de texte (voir typographie), en
        italique léger.

## 2. Palette de couleurs

  -----------------------------------------------------------------------
  Usage                    Couleur principale            Variantes
  ------------------------ ----------------------------- ----------------
  Primaire                 Bleu Tchalanet `#0052CC`      Hover :
                                                         `#0040A8`, Clair
                                                         : `#E6F0FF`

  Secondaire               Blanc pur `#FFFFFF`           Gris clair
                                                         `#F5F7FA`

  Accent 1                 Jaune loto `#FFD600`          Hover :
                                                         `#FFCC00`

  Accent 2                 Vert gain `#00C853`           Hover :
                                                         `#00B342`

  Sombre (UI)              Bleu nuit `#0D1B2A`           Gris foncé
                                                         `#1C2833`
  -----------------------------------------------------------------------

-   **Dégradés autorisés** :
    -   Primaire → Blanc (haut en bas).\
    -   Jaune → Vert (pour gains/jackpot).
-   **Contrastes** : respecter un ratio minimum 4.5:1 (AA WCAG).

## 3. Typographie

-   **Titres & branding** : *Poppins* (sans-serif, bold).\
-   **Texte courant** : *Roboto* (sans-serif, regular).\
-   **Chiffres (tickets, tirages, jackpots)** : *Roboto Mono*.\
-   **Hiérarchie** :
    -   H1 : 32px, bold, couleur primaire.\
    -   H2 : 24px, semibold.\
    -   H3 : 18px, regular.\
    -   Texte : 16px.\
    -   Caption/Labels : 12--14px.

## 4. Iconographie

-   Pack : [Lucide](https://lucide.dev/) ou Material Icons.\
-   Style : traits fins, 2px, arrondis.\
-   Code couleur :
    -   Actions principales : bleu primaire.\
    -   Succès/validation : vert.\
    -   Erreur : rouge `#E53935`.

## 5. UI Components (référentiel)

-   **Boutons** :
    -   Primaire : fond bleu, texte blanc, arrondi `rounded-2xl`.\
    -   Secondaire : fond blanc, bord bleu, texte bleu.\
    -   Danger : fond rouge, texte blanc.\
-   **Inputs/Formulaires** :
    -   Bord 1px gris clair, focus outline bleu.\
    -   Labels au-dessus, 14px, Roboto.\
-   **Tables** :
    -   En-têtes fond gris clair.\
    -   Ligne survolée : `#E6F0FF`.\
-   **Cartes/Dashboards** :
    -   Fond blanc, shadow doux, arrondi `rounded-2xl`.\
    -   Padding minimum 16px.

## 6. Visuels & Illustrations

-   **Thème loto** : boules numérotées, animations discrètes en
    background.\
-   **Illustrations** : style *flat design*, couleurs cohérentes avec
    palette.\
-   **Photos** : si utilisées, traitement léger (saturation -10%,
    luminosité +5%).

## 7. Accessibilité

-   Navigation clavier obligatoire.\
-   Respect des contrastes (voir §2).\
-   Police min 16px.\
-   Feedback visuel + sonore optionnel pour actions critiques (ex.
    validation ticket).

## 8. Usage cross-plateforme

-   **Web (Angular)** : Tailwind + DaisyUI thèmes (`tchalanet-light`,
    `tchalanet-dark`).\
-   **Mobile (Flutter)** : utiliser MaterialTheme avec mapping palette.\
-   **Docs & PDF** : appliquer les couleurs primaires pour titres,
    secondaire pour fonds, police Roboto.
