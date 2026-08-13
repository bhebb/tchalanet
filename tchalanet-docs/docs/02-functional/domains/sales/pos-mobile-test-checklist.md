# POS Mobile - Strategie de test terrain

Objectif: valider le flux vendeur POS de bout en bout avant merge/release: Maryaj gratis, vente directe, impression, PDF, Sunmi/ESC-POS, et verification ticket public/mobile.

## 1. Preparation admin

| Cas | Admin fait ca | Vendeur fait ca | Resultat attendu |
| --- | --- | --- | --- |
| Terminal POS par defaut | Ouvre le terminal vendeur dans admin, verifie `Impression automatique = ON`, `Vente rapide = ON`, `Mode impression = POS`, `Papier = 58mm`. | Se connecte avec le terminal. | Les parametres remontent dans l'app POS sans configuration technique par le vendeur. |
| Client sans Sunmi | Configure le terminal en POS/Bluetooth 58mm ou PDF manuel selon materiel disponible. | Ouvre Parametres et choisit l'imprimante Bluetooth si besoin. | Le vendeur peut imprimer sans Sunmi; PDF reste manuel, jamais auto en plein flux POS. |
| Admin ouvre POS d'un vendeur | Depuis la liste des seller terminals, clique `Ouvrir POS`. | Rien. | Le contexte utilise le terminal cible, pas le telephone/admin courant. |
| Langue terminal | Met le tenant/terminal en HT, FR, puis EN selon le scenario. | Imprime un ticket test. | Le ticket imprime dans la bonne langue via `buyerLocale`. |
| Base URL verification | Verifie la config publique/local env pour l'URL QR. | Imprime un ticket et scanne le QR. | Le QR pointe vers la bonne page de verification, pas une URL prod par erreur. |

## 2. Maryaj gratis et vente directe

| Cas | Admin fait ca | Vendeur fait ca | Resultat attendu |
| --- | --- | --- | --- |
| Promo active | Active/configure Maryaj gratis pour le tenant/terminal. | Vend un ticket eligible. | Le backend prepare une ligne Maryaj gratis avant confirmation. |
| Vente rapide avec promo | Laisse `Vente rapide = ON`. | Appuie une seule fois pour vendre. | Si Maryaj gratis est genere, l'app reste en preview; elle ne confirme pas automatiquement. |
| Regeneration | Rien de plus. | Sur la preview, appuie `Regenerer/Jenere anko`. | La ligne promo change, le ticket n'existe pas encore, le total reste coherent. |
| Limite regeneration | Configure ou simule une limite atteinte. | Tente de regenerer apres limite. | Le bouton disparait ou l'erreur backend est affichee; la derniere preview reste intacte. |
| Confirmation apres regeneration | Rien. | Confirme apres avoir choisi la derniere generation. | Le ticket cree contient exactement la ligne affichee, sans regeneration cachee. |
| Prepare echoue | Simule stock/limite/validation impossible. | Lance vente rapide. | Pas de confirm appele, pas de ticket cree, erreur claire. |
| Confirm echoue apres prepare | Simule reseau/401/stock/limite au confirm. | Lance vente directe. | Pas de faux succes visuel; l'intention reste en etat non resolu/retry propre. |
| Idempotence | Simule replay confirm. | Retente apres timeout. | Pas de double ticket; l'app recharge le ticket existant si deja confirme. |

## 3. Impression POS, PDF, Sunmi

| Cas | Admin fait ca | Vendeur fait ca | Resultat attendu |
| --- | --- | --- | --- |
| ESC/POS Bluetooth 58mm | Configure `Mode impression = POS`, `Papier = 58mm`. | Selectionne l'imprimante Bluetooth puis imprime un ticket. | Ticket sort sans quitter l'app; QR scannable. |
| Alignement lignes | Rien. | Vend un ticket avec plusieurs lignes meme jeu. | Bloc jeu lisible; en-tete `No` et `Mise` alignes avec les lignes dessous sur 58mm. |
| Plusieurs jeux | Rien. | Vend un ticket avec plusieurs jeux/blocs. | Chaque jeu a son bloc; total correct; pas de chevauchement texte. |
| Langue print | Change langue terminal ou app. | Imprime en HT, FR, EN. | Libelles ticket dans la langue attendue; pas de melange involontaire. |
| Print auto apres vente | Active impression auto. | Confirme une vente sans erreur. | Impression directe apres creation ticket; l'app reste sur le flux POS. |
| Printer indisponible | Eteint/deconnecte imprimante. | Vend ou reimprime. | Message lisible; pas de stacktrace brute; ticket reste accessible/reimprimable. |
| PDF admin/A4 | Depuis admin ou action volontaire PDF, choisit telechargement PDF. | Aucun ou ouvre le PDF volontairement. | PDF A4 correct pour admin; ce mode ne remplace pas l'app dans le flux auto POS. |
| Sunmi | Configure terminal Sunmi/POS integre si disponible. | Vend un ticket sur Sunmi. | Impression integree 58mm sans pairing Bluetooth; meme contenu que ESC/POS. |
| Reprint historique | Rien. | Depuis historique, appuie imprimer/reimprimer avec raison. | Reprint fonctionne, raison auditee, langue et QR corrects. |

## 4. Verification ticket public et mobile

| Cas | Admin fait ca | Vendeur fait ca | Resultat attendu |
| --- | --- | --- | --- |
| Public code manuel | Rien. | Sur page publique, entre le code public. | Le service public retourne le statut du ticket. |
| Public QR URL | Rien. | Scanne le QR avec camera telephone hors app. | Le navigateur ouvre la page publique et affiche le resultat. |
| Mobile code manuel | Rien. | Dans POS, appuie `Verifye yon tike`, entre le code. | L'app appelle `/tenant/cashier/tickets/verify`, pas le service public. |
| Camera interne | Rien. | Dans POS, lance scanner camera interne et scanne le QR. | L'app lit l'URL/code et affiche la page resultat interne. |
| Permission camera refusee | Rien. | Refuse la permission camera. | L'app propose saisie manuelle; pas de blocage. |
| Camera externe vers app | Configure Android App Links/deep link. | Scanne QR avec camera systeme. | Le lien ouvre l'app; route vers verification POS. |
| Logged out deep link | Rien. | Scanne QR alors que l'app POS est deconnectee. | L'app stocke le code pending, demande login, puis redirige vers verification. |
| Ticket pending draw | Rien. | Verifie un ticket avant tirage. | Statut clair: pas payable/en attente tirage. |
| Ticket perdant | Publie resultat perdant. | Verifie le ticket. | Statut clair: non gagnant, pas d'action paiement. |
| Ticket gagnant/payable | Publie resultat gagnant. | Verifie le ticket. | Montant gagne affiche a titre indicatif si disponible; action suivante claire. |
| Ticket deja paye/annule/expire | Prepare chaque statut si backend supporte. | Verifie le ticket. | Message specifique; pas de paiement double. |

## 5. Parcours mobile et resilience

| Cas | Admin fait ca | Vendeur fait ca | Resultat attendu |
| --- | --- | --- | --- |
| Acces Parametres | Rien. | Ouvre Parametres puis revient. | Retour possible vers accueil; pas d'impasse navigation. |
| Home POS | Rien. | Ouvre home POS. | Bouton `Verifye yon tike` visible et comprehensible. |
| Changement terminal/login | Change ou ouvre un autre terminal. | Logout/login avec autre terminal. | La config imprimante/terminal ne fuit pas entre vendeurs. |
| Reseau coupe apres prepare | Simule perte reseau. | Lance vente directe. | Pas de faux ticket imprime; etat retry/erreur propre. |
| App en background | Met app en background pendant/avant print ou scan. | Revient dans l'app. | Etat coherent, pas de double confirm/print automatique. |

## Priorite de validation avant merge

1. Maryaj gratis: prepare, preview, regeneration, confirm exact.
2. Impression: ESC/POS 58mm, alignement `No/Mise`, langue, QR correct.
3. Verification: public code/URL, mobile POS code/scan, logged-out deep link.
4. Resilience: confirm fail, printer fail, navigation parametres.
