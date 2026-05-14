package com.tchalanet.server.core.limitpolicy.api;

public enum RuleKey {

    // Montant maximum autorisé pour une seule ligne de ticket.
    // Exemple Haïti : empêcher qu’un client mette 5 000 HTG directement sur un seul numéro comme "05".
    MAX_STAKE_PER_LINE,

    // Nombre maximum de lignes autorisées sur un ticket.
    // Exemple Haïti : éviter qu’un ticket contienne 200 combinaisons et devienne difficile à contrôler/imprimer.
    MAX_LINES_PER_TICKET,

    // Montant total maximum autorisé pour tout le ticket.
    // Exemple Haïti : limiter un ticket complet à 10 000 HTG, même si chaque ligne respecte sa limite individuelle.
    MAX_STAKE_PER_TICKET,

    // Mise totale maximum déjà exposée sur une sélection pour un tirage donné.
    // Exemple Haïti : limiter le total vendu sur "05" pour le tirage New York Midi afin d’éviter un numéro trop chargé.
    MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW,

    // Gain potentiel total maximum déjà exposé sur une sélection pour un tirage donné.
    // Exemple Haïti : même si les mises sur "05" semblent faibles, bloquer si le payout potentiel devient trop risqué.
    MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW,

    // Mise maximum par type de pari dans un même ticket.
    // Exemple Haïti : autoriser 1 000 HTG sur bolet simple, mais seulement 300 HTG sur mariage ou lotto plus risqué.
    MAX_STAKE_PER_BET_TYPE_PER_TICKET,

    // Mise maximum sur une même sélection dans un même ticket.
    // Exemple Haïti : si le client répète "05" plusieurs fois dans le ticket, limiter le total cumulé sur "05".
    MAX_STAKE_PER_SELECTION_PER_TICKET,

    // Gain potentiel maximum pour tout le ticket.
    // Exemple Haïti : un ticket de 500 HTG peut produire un payout énorme selon les cotes; cette règle limite ce risque global.
    MAX_POTENTIAL_PAYOUT_PER_TICKET,

    // Gain potentiel maximum pour une seule ligne de ticket.
    // Exemple Haïti : éviter qu’une seule ligne "12-34" en mariage crée à elle seule un paiement trop élevé.
    MAX_POTENTIAL_PAYOUT_PER_LINE,

    // Nombre maximum de ventes déjà enregistrées sur une sélection pour un tirage donné.
    // Exemple Haïti : si trop de clients jouent "05" sur le même tirage, la sélection peut être ralentie ou bloquée.
    MAX_SALES_COUNT_PER_SELECTION_PER_DRAW,

    // Nombre maximum de ventes/lignes dans un ticket.
    // Exemple Haïti : limiter le nombre d’entrées vendues sur un ticket pour éviter les tickets trop volumineux ou abusifs.
    // Note : proche de MAX_LINES_PER_TICKET; à garder seulement si "sales count" a un sens différent de "lines count" dans ton modèle.
    MAX_SALES_COUNT_PER_TICKET,

    // Bloque une ou plusieurs sélections pour un tirage donné.
    // Exemple Haïti : fermer temporairement "05" sur Florida Soir si le numéro est trop joué ou jugé à risque.
    BLOCK_SELECTION_PER_DRAW,

    // Bloque complètement un type de pari.
    // Exemple Haïti : désactiver temporairement mariage, lotto3 ou lotto4 pour un outlet, terminal ou tenant.
    BLOCK_BET_TYPE,

    // Nombre maximum de tickets qu’un agent peut vendre dans une fenêtre de temps.
    // Exemple Haïti : empêcher un vendeur de sortir 300 tickets en 5 minutes en cas de suspicion ou d’erreur opérationnelle.
    MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW,

    // Mise totale maximum qu’un agent peut vendre sur un tirage donné.
    // Exemple Haïti : limiter l’exposition totale créée par un agent sur New York Midi.
    MAX_STAKE_PER_AGENT_PER_DRAW,

    // Mise totale maximum qu’un outlet peut vendre sur un tirage donné.
    // Exemple Haïti : limiter l’exposition globale d’un point de vente sur un tirage très populaire.
    MAX_STAKE_PER_OUTLET_PER_DRAW
}
