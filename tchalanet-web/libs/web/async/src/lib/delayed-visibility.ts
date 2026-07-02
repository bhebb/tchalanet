import { DestroyRef, Signal, effect, inject, signal } from '@angular/core';

export interface TchDelayedVisibilityOptions {
  /** Délai avant d'afficher l'indicateur (anti-flash des réponses rapides). */
  readonly showDelayMs?: number;
  /** Durée minimale d'affichage une fois montré (anti-flicker). */
  readonly minVisibleMs?: number;
}

/**
 * Visibilité anti-flash d'un indicateur de chargement : n'apparaît qu'après
 * `showDelayMs` (~300 ms) et reste au moins `minVisibleMs` (~500 ms).
 * À créer dans un contexte d'injection.
 */
export function delayedVisibility(
  active: () => boolean,
  options: TchDelayedVisibilityOptions = {},
): Signal<boolean> {
  const showDelay = options.showDelayMs ?? 300;
  const minVisible = options.minVisibleMs ?? 500;

  const visible = signal(false);
  let showTimer: ReturnType<typeof setTimeout> | null = null;
  let hideTimer: ReturnType<typeof setTimeout> | null = null;
  let shownAt = 0;

  inject(DestroyRef).onDestroy(() => {
    if (showTimer !== null) clearTimeout(showTimer);
    if (hideTimer !== null) clearTimeout(hideTimer);
  });

  effect(() => {
    if (active()) {
      if (hideTimer !== null) {
        clearTimeout(hideTimer);
        hideTimer = null;
      }
      if (!visible() && showTimer === null) {
        showTimer = setTimeout(() => {
          showTimer = null;
          shownAt = Date.now();
          visible.set(true);
        }, showDelay);
      }
    } else {
      if (showTimer !== null) {
        clearTimeout(showTimer);
        showTimer = null;
      }
      if (visible() && hideTimer === null) {
        const wait = Math.max(0, minVisible - (Date.now() - shownAt));
        if (wait === 0) {
          visible.set(false);
        } else {
          hideTimer = setTimeout(() => {
            hideTimer = null;
            visible.set(false);
          }, wait);
        }
      }
    }
  });

  return visible.asReadonly();
}
