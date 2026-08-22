# Guide client - configurer l'imprimante POS

Ce guide aide un vendeur ou un responsable boutique à vérifier l'impression des tickets Tchalanet.

## Modes supportés

| Cas | Ce qu'il faut faire |
|---|---|
| Terminal Sunmi avec imprimante intégrée | Rien à installer. L'app Tchalanet détecte l'imprimante interne et imprime en 58 mm. |
| Autre terminal Android avec RawBT | Installer RawBT, configurer l'imprimante dans RawBT, puis imprimer depuis Tchalanet. |
| Imprimante Bluetooth ESC/POS | Appairer l'imprimante dans Android, puis la sélectionner dans les paramètres imprimante Tchalanet. |
| Dépannage uniquement | Utiliser le reçu PDF, SMS ou email si l'impression directe n'est pas disponible. |

Le format du reçu vient des paramètres du terminal Tchalanet. Par défaut, les terminaux POS utilisent du papier 58 mm. Certaines imprimantes externes peuvent utiliser du 80 mm si ce format est configuré sur le terminal.

Exception : une imprimante interne Sunmi avec rouleau fixe reçoit toujours un reçu 58 mm, même si un autre format a été choisi par erreur.

## Sunmi avec imprimante intégrée

1. Ouvrir le capot imprimante.
2. Mettre un rouleau thermique 58 mm.
3. Vérifier que la face thermique du papier est du bon côté.
4. Fermer le capot correctement.
5. Faire un test print depuis l'app.
6. Faire une vente test et imprimer le ticket.

Sur Sunmi, l'imprimante interne n'apparaît pas comme une imprimante Bluetooth. C'est normal : l'app Tchalanet communique directement avec le service imprimante Sunmi.

Le support Sunmi n'est pas limité au V2 Pro. Les générations Sunmi qui exposent le service imprimante interne compatible sont prises en charge par le même mode.

## RawBT

RawBT est le mode de secours recommandé quand le terminal n'est pas un Sunmi certifié ou quand l'imprimante est externe.

1. Installer RawBT sur le terminal Android.
2. Connecter l'imprimante dans RawBT.
3. Lancer un test depuis RawBT.
4. Ouvrir Tchalanet et imprimer le ticket.

Si RawBT n'est pas installé, Tchalanet ne peut pas utiliser ce mode.

## Secours sans imprimante

Si aucune impression ne fonctionne, le ticket peut être remis au client par un autre canal configuré par l'opérateur :

- SMS ;
- email ;
- reçu PDF ;
- code de vérification affiché à l'écran.

Le vendeur doit toujours vérifier que le ticket a bien un code de vérification avant de remettre l'information au client.

## Papier et encre

Ces imprimantes utilisent du papier thermique. Il n'y a pas de cartouche d'encre.

Si le ticket sort blanc ou trop clair :

- retourner le rouleau de papier ;
- vérifier que le papier est bien du papier thermique ;
- fermer le capot fermement ;
- charger le terminal ;
- nettoyer doucement la tête thermique si elle est sale ;
- refaire un test print.

## Problèmes fréquents

| Symptôme | Cause probable | Action |
|---|---|---|
| Le premier ticket est faible, le deuxième est bon | Imprimante ou service interne encore froid | Attendre une seconde et réimprimer si nécessaire. L'app prépare l'imprimante avant chaque ticket. |
| Aucun texte ne sort | Papier à l'envers ou papier non thermique | Retourner le rouleau ou changer de papier. |
| Le test Sunmi marche mais pas le ticket | Driver ou format ESC/POS incompatible | Mettre à jour l'app Tchalanet et retester. |
| L'imprimante n'apparaît pas dans Bluetooth | Imprimante interne Sunmi | Normal, elle est détectée automatiquement par l'app. |
| RawBT ne reçoit rien | RawBT absent ou non configuré | Installer/configurer RawBT puis relancer l'impression. |
| Le QR code est coupé | Papier mal avancé ou capot mal fermé | Fermer le capot et réimprimer. |
| Le client doit partir sans reçu papier | Imprimante indisponible | Envoyer le ticket par SMS/email ou donner le code de vérification. |

## Informations à envoyer au support

En cas de problème, envoyer :

- modèle du terminal ;
- nom ou code du terminal Tchalanet ;
- heure du test ;
- code du ticket ;
- photo du ticket imprimé ;
- préciser si le test print fonctionne ;
- préciser si c'est la première impression ou une réimpression.
