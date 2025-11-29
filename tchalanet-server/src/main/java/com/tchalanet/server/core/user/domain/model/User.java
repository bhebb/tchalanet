package com.tchalanet.server.core.user.domain.model;

import java.util.Objects;
import lombok.Getter;
import lombok.ToString;

/** Modèle de base d'un utilisateur applicatif côté domaine. */
@Getter
@ToString
public class User {

  private final UserId id;
  private final String email;

  // TODO: ajouter rôles applicatifs (vendeur, admin, superadmin...), nom complet, etc.

  public User(UserId id, String email) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.email = email; // peut être null/optionnel selon ton modèle
  }
}
