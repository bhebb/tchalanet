# Guide du Vendeur — Application Caisse Tchalanet

> Guide pratique pour les vendeurs qui utilisent l'application mobile ou web POS.  
> Pas de technique — juste les étapes pour vendre, imprimer et gérer les tickets.

---

## Avant de commencer

Pour pouvoir vendre, il faut que votre administrateur ait déjà :

1. Créé votre compte vendeur
2. Assigné votre compte à un point de vente
3. Configuré et activé votre terminal

Si l'une de ces étapes n'est pas faite, contacter l'administrateur du tenant.

---

## 1. Se connecter

Ouvrir l'application et entrer votre nom d'utilisateur et mot de passe.

L'application va charger votre profil : votre nom, votre point de vente, votre langue et les préférences d'impression.

> Si la connexion échoue, vérifier votre mot de passe. Si le problème persiste, contacter votre administrateur.

---

## 2. Ouvrir une session de vente

Au début de chaque journée ou de chaque quart de travail, il faut ouvrir une session.

**Étapes :**
1. Sur l'écran principal, appuyer sur **Ouvrir la session**
2. Entrer le montant en caisse au début (la "float") — ex: 500 HTG
3. Confirmer

L'application affiche la session comme **ouverte** avec l'heure de début.

> Sans session ouverte, la vente de tickets est bloquée.

---

## 3. Vérifier le contexte opérationnel

L'application vérifie automatiquement que votre terminal et votre point de vente sont actifs.

- Si le contexte est **valide** : vous pouvez commencer à vendre.
- Si le contexte est **invalide** : un message s'affiche. Contacter votre administrateur.

---

## 4. Vendre un ticket

### 4.1 Choisir un tirage

Sur l'écran de vente, la liste des tirages disponibles s'affiche.

- Choisir le tirage souhaité (ex: "Haïti • Texas • 10:00")
- Seuls les tirages **ouverts** sont disponibles à la vente
- La vente se ferme automatiquement quelques secondes avant l'heure limite

### 4.2 Saisir le ticket

Choisir le jeu (Bolet, Maryaj, Loto 3, etc.) et entrer la sélection du client :

| Jeu | Ce que le client choisit |
|---|---|
| Bolet | 2 chiffres (ex: 11) |
| Maryaj | 2 paires de chiffres (ex: 21-25) |
| Loto 3 | 3 chiffres (ex: 012) |
| Loto 4 | 4 chiffres (ex: 1234) |
| Loto 5 | 5 chiffres (ex: 12345) |

Entrer la mise du client.

### 4.3 Valider avant de vendre

L'application vérifie le ticket automatiquement. Trois résultats possibles :

| Résultat | Que faire |
|---|---|
| **Peut être vendu** | Appuyer sur "Vendre" |
| **À modifier** | Lire le message affiché et corriger le ticket |
| **Refusé** | Le tirage est fermé ou la mise dépasse les limites — impossible de vendre ce ticket |

### 4.4 Confirmer la vente

Appuyer sur **Vendre**.

L'application affiche immédiatement le **code du ticket** en grand — ex: `40CP-JBMR`.

**Important** : donner ce code au client ou imprimer le ticket **avant de passer à la vente suivante**. Ce code est la preuve de la vente même sans impression.

---

## 5. Remettre le ticket au client

Plusieurs options :

- **Imprimer** : appuyer sur "Imprimer" — le ticket sort de l'imprimante thermique
- **Envoyer par SMS** : entrer le numéro de téléphone du client et appuyer "Envoyer"
- **Envoyer par WhatsApp** : même chose sur WhatsApp
- **Copier le code** : copier et partager manuellement

> Le client peut vérifier son ticket en scannant le code QR ou en entrant le code sur le site Tchalanet.

---

## 6. Annuler un ticket

Un ticket peut être annulé dans les **3 minutes suivant la vente**.

**Étapes :**
1. Aller dans la liste des tickets vendus
2. Trouver le ticket à annuler
3. Appuyer sur **Annuler**
4. Confirmer la raison

> Après 3 minutes, l'annulation n'est plus possible depuis l'application. Contacter votre administrateur.

---

## 7. Consulter les tickets vendus

Depuis l'écran principal, appuyer sur **Mes tickets** pour voir tous les tickets de la session en cours.

Informations disponibles : code du ticket, tirage, montant, heure de vente, statut.

---

## 8. Vente sans internet (mode hors-ligne)

Si la connexion internet est coupée, l'application peut continuer à vendre en mode hors-ligne **si votre terminal a un accès hors-ligne activé**.

En mode hors-ligne :
- Les ventes sont enregistrées localement
- Les codes sont générés depuis les codes offline attribués à votre terminal
- Les ventes sont transmises au serveur dès que la connexion est rétablie

> Le mode hors-ligne a des limites : nombre maximal de tickets et montant maximal définis par votre administrateur. Quand les limites sont atteintes, la vente s'arrête jusqu'à synchronisation.

---

## 9. Fermer la session

En fin de journée ou de quart, fermer la session :

1. Appuyer sur **Fermer la session**
2. Entrer le montant en caisse à la fermeture
3. Confirmer

L'application affiche le résumé de la session (nombre de tickets, montant total).

---

## 10. Messages d'erreur courants

| Message | Cause probable | Que faire |
|---|---|---|
| "Session fermée" | La session POS n'est pas ouverte | Ouvrir une session |
| "Tirage fermé" | Le tirage n'accepte plus de ventes | Choisir un autre tirage |
| "Mise trop élevée" | La mise dépasse votre limite | Réduire la mise |
| "Limite atteinte pour ce numéro" | Ce numéro a atteint sa limite pour ce tirage | Proposer un autre numéro au client |
| "Contactez votre administrateur" | Problème de configuration du terminal ou du contexte | Appeler l'administrateur du tenant |
| "Connexion refusée" | JWT expiré ou compte désactivé | Fermer l'app et se reconnecter |

---

## 11. Contacts utiles

- **Problème de terminal ou de compte** → administrateur de votre tenant
- **Bug dans l'application** → signaler via le canal support Tchalanet

---

## Rappel : les 4 règles essentielles

1. **Toujours ouvrir une session** avant de vendre
2. **Donner le code au client immédiatement** après la vente — même sans impression
3. **Annuler dans les 3 minutes** si le client change d'avis
4. **Fermer la session** en fin de service
