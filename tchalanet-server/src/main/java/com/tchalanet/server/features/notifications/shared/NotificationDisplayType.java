package com.tchalanet.server.features.notifications.shared;

/** Type d'affichage d'une notification dans l'UI. */
public enum NotificationDisplayType {
  BANNER, // bandeau en haut (ex: sous le header)
  TOAST, // petit pop en bas ou en haut, qui disparaît
  MODAL, // bloquant, demande une action
  PUSH, // notifs "push" (web/mobile) – si/plus tard
  INBOX_ONLY // seulement dans la liste, pas d'affichage instantané
}
