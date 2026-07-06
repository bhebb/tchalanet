import { AdminStatusTone } from '@tch/ui/console';
import { BadgeStatus } from '@tch/ui/components';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

export type ConsoleGameCardBadgeTone = BadgeStatus | 'info' | 'neutral';

export interface ConsoleGameRow {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly logoUrl?: string | null;
  readonly category?: string | null;
  readonly sortOrder?: number | null;
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly supporting?: string | null;
  readonly actions?: readonly ConsoleRowAction[];
}

export interface ConsoleGameCardSummaryItem {
  readonly icon: string;
  readonly label: string;
  readonly warning?: boolean;
}

export interface ConsoleGameCardView {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly logoUrl?: string | null;
  readonly logoText?: string | null;
  readonly statusLabel: string;
  readonly statusTone: AdminStatusTone;
  readonly badgeLabel?: string | null;
  readonly badgeTone?: ConsoleGameCardBadgeTone;
  readonly unavailable?: boolean;
  readonly unavailableLabel?: string | null;
  readonly summaryItems?: readonly ConsoleGameCardSummaryItem[];
  readonly actions?: readonly ConsoleRowAction[];
  readonly secondaryActions?: readonly ConsoleRowAction[];
}

export interface ConsoleGameActionEvent {
  readonly row: ConsoleGameRow;
  readonly action: ConsoleRowAction;
}

export interface ConsoleGameCardActionEvent {
  readonly row: ConsoleGameCardView;
  readonly action: ConsoleRowAction;
}
