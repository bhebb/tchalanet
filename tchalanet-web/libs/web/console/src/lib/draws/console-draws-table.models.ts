import { AdminStatusTone } from '@tch/ui/console';
import { ConsoleDrawSlotIdentity } from '../draw-slots/console-draw-slot-identity.models';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

export interface ConsoleDrawRow {
  readonly id: string;
  readonly groupLabel?: string;
  readonly title: string;
  readonly subtitle?: string;
  readonly meta?: string;
  readonly logoUrl?: string | null;
  readonly logoAlt?: string;
  readonly logoText?: string;
  readonly identity: ConsoleDrawSlotIdentity;
  readonly scheduledDateLabel?: string;
  readonly scheduledTimeLabel?: string;
  readonly countdownLabel?: string | null;
  readonly countdownTone?: 'default' | 'warning';
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly resultLabel?: string;
  readonly resultTone?: AdminStatusTone;
  readonly resultNumbers?: readonly string[];
  readonly resultHint?: string;
  /**
   * Message orienté action pour un résultat pas encore disponible (ex. « Résultat à saisir »),
   * affiché sur la carte mobile à la place du badge de statut brut. Laisser vide pour garder le
   * rendu badge existant — ce champ est propre au rendu carte, il n'affecte ni le tableau desktop
   * ni la page de détail, qui continuent de lire `resultLabel`.
   */
  readonly resultActionHint?: string;
  readonly modeLabel?: string;
  readonly publicationLabel?: string;
  readonly publicationTone?: AdminStatusTone;
  readonly actions?: readonly ConsoleRowAction[];
  readonly pending?: boolean;
}

export interface ConsoleDrawActionEvent {
  readonly row: ConsoleDrawRow;
  readonly action: ConsoleRowAction;
}

export interface ConsoleDrawSelectionEvent {
  readonly row: ConsoleDrawRow;
  readonly selected: boolean;
}
