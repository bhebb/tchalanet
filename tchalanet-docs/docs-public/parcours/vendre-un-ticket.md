# Parcours rapide — Vendre un ticket

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket, reçu ou montant n'a de valeur réelle.  
    **Version testée : à renseigner avant livraison**  
    **Dernière mise à jour : 2026-07-23**

Ce parcours est fait pour un testeur POS. Il montre le chemin attendu pour une
vente simple, sans entrer dans les détails techniques.

## Captures à préparer

Ajoutez les captures de la séance de validation dès qu'elles sont disponibles :

| Capture | Écran attendu | Pourquoi |
|---|---|---|
| 1 | Connexion terminal | Vérifier terminal, PIN et message d'accès. |
| 2 | Liste des tirages vendables | Vérifier qu'un tirage fermé n'est pas vendable. |
| 3 | Saisie de mise | Vérifier clavier, champs et total. |
| 4 | Récapitulatif avant confirmation | Vérifier la double confirmation. |
| 5 | Reçu ou ticket confirmé | Vérifier code ticket, tirage, montant et terminal. |

<div class="tch-flow">
  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 1</div>
    <h3>Ouvrir l'application POS</h3>
    <p>Le vendeur ouvre l'application mobile staging et arrive sur l'écran de connexion terminal.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 2</div>
    <h3>Se connecter au terminal</h3>
    <p>Le vendeur utilise le terminal de test et son PIN. Si un changement de PIN est demandé, il le fait avant de continuer.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 3</div>
    <h3>Choisir un tirage ouvert</h3>
    <p>Le POS affiche seulement les tirages vendables. Un tirage fermé ne doit pas permettre la vente.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 4</div>
    <h3>Ajouter une mise</h3>
    <p>Le vendeur saisit une mise simple et vérifie le total avant confirmation.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 5</div>
    <h3>Confirmer la vente</h3>
    <p>Le vendeur vérifie le récapitulatif, confirme l'action, puis confirme une seconde fois si l'application le demande. Après confirmation, un code ticket apparaît.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 6</div>
    <h3>Imprimer ou afficher le reçu</h3>
    <p>Le reçu doit afficher le terminal, le tirage, les mises, le total et le code ticket.</p>
  </section>
</div>

## Ce que vous devez voir

| Moment | Résultat attendu |
|---|---|
| Connexion | Le bon terminal est affiché |
| Tirage | Le tirage est ouvert et vendable |
| Mise | Le total est clair avant confirmation |
| Confirmation | Un code ticket est créé |
| Reçu | Les informations correspondent à la vente |

## Si ça bloque

Utilisez [Signaler un problème](../validation/signaler-un-probleme.md) avec :

- rôle : `Vendeur/POS` ;
- scénario : `POS-SALE-01` ;
- terminal utilisé ;
- tirage ;
- mise saisie ;
- code ticket si disponible ;
- capture ou photo du reçu.

<div class="tch-check">
Objectif du parcours : prouver qu'un vendeur peut vendre un ticket simple et
retrouver une preuve claire de la vente.
</div>
