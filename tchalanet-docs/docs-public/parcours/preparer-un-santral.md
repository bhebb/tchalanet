# Parcours rapide — Préparer un santral

!!! warning "Environnement de validation"
    **Environnement : STAGING**  
    **Données : tests uniquement**  
    Aucun ticket, reçu ou montant n'a de valeur réelle.  
    **Version testée : à renseigner avant livraison**  
    **Dernière mise à jour : 2026-07-23**

Ce parcours est fait pour un administrateur ou un propriétaire. Il aide à vérifier que le santral
est prêt pour une vente POS de test.

<div class="tch-flow">
  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 1</div>
    <h3>Ouvrir la configuration générale</h3>
    <p>Les blocs obligatoires doivent être lisibles. Les blocages doivent expliquer la prochaine action.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 2</div>
    <h3>Vérifier les informations du santral</h3>
    <p>Nom, adresse et informations utiles doivent être corrects ou modifiables selon permissions.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 3</div>
    <h3>Vérifier les jeux</h3>
    <p>Les jeux que le vendeur doit vendre sont actifs et visibles au POS.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 4</div>
    <h3>Vérifier les canaux de tirage</h3>
    <p>Les canaux nécessaires sont configurés et leur statut est compréhensible.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 5</div>
    <h3>Vérifier le terminal POS</h3>
    <p>Au moins un terminal de test doit être visible et utilisable pour la connexion mobile.</p>
  </section>

  <section class="tch-step">
    <div class="tch-step__eyebrow">Étape 6</div>
    <h3>Lancer une vente de test</h3>
    <p>Le santral est prêt lorsque le vendeur peut se connecter et confirmer une vente simple.</p>
  </section>
</div>

## Ce que vous devez voir

| Zone | Résultat attendu |
|---|---|
| Configuration générale | Obligatoire et optionnel sont séparés clairement |
| Informations santral | Données compréhensibles |
| Jeux | Aucun doublon évident, statut clair |
| Canaux | Canaux nécessaires visibles |
| Terminal POS | Terminal actif ou action de configuration claire |
| Vente test | Ticket créé depuis le POS |

## Si ça bloque

Utilisez [Signaler un problème](../validation/signaler-un-probleme.md) avec :

- rôle : `Administrateur` ou `Propriétaire` ;
- page bloquante ;
- bloc ou bouton concerné ;
- résultat attendu ;
- capture de la configuration générale ou du terminal.

<div class="tch-check">
Objectif du parcours : confirmer qu'un santral est opérationnel pour une vente
de test, sans dépendre d'une explication technique.
</div>
