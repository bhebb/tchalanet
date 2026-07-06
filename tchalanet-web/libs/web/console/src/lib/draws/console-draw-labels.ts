import { AdminStatusTone } from '@tch/ui/console';

import {
  ConsoleDrawLifecycleDisplayAction,
  ConsoleDrawPublicationStatus,
  ConsoleDrawResultQuality,
  ConsoleDrawResultStatus,
  ConsoleDrawSalesStatus,
  ConsoleDrawStatus,
} from '../domain/console-domain-types';

export function consoleDrawStatusLabel(status: ConsoleDrawStatus | string | null | undefined): string {
  if (!status) return '—';
  switch (status) {
    case 'SCHEDULED': return 'Planifié';
    case 'OPEN': return 'Ouvert';
    case 'LOCKED': return 'Verrouillé';
    case 'CLOSED': return 'Fermé';
    case 'PENDING_RESULTS': return 'Résultats attendus';
    case 'RESULTED': return 'Résultat appliqué';
    case 'RESULTS_APPLIED': return 'Résultat appliqué';
    case 'SETTLED': return 'Réglé';
    case 'CANCELED':
    case 'CANCELLED': return 'Annulé';
    case 'ARCHIVED': return 'Archivé';
    default: return status;
  }
}

export function consoleDrawStatusTone(status: ConsoleDrawStatus | string | null | undefined): AdminStatusTone {
  switch (status) {
    case 'OPEN': return 'success';
    case 'RESULTED':
    case 'RESULTS_APPLIED':
    case 'SETTLED': return 'info';
    case 'LOCKED':
    case 'PENDING_RESULTS':
    case 'SCHEDULED': return 'warning';
    case 'CANCELED':
    case 'CANCELLED': return 'danger';
    case 'ARCHIVED': return 'neutral';
    default: return 'neutral';
  }
}

export function consoleDrawSalesStatusLabel(status: ConsoleDrawSalesStatus | string | null | undefined): string {
  if (!status) return '—';
  switch (status) {
    case 'OPEN': return 'Ouvert';
    case 'CLOSED': return 'Fermé';
    case 'LOCKED': return 'Verrouillé';
    case 'CANCELLED': return 'Annulé';
    case 'UPCOMING': return 'À venir';
    default: return status;
  }
}

export function consoleDrawSalesStatusTone(status: ConsoleDrawSalesStatus | string | null | undefined): AdminStatusTone {
  switch (status) {
    case 'OPEN': return 'success';
    case 'LOCKED': return 'warning';
    case 'CANCELLED': return 'danger';
    default: return 'neutral';
  }
}

export function consoleDrawResultStatusLabel(status: ConsoleDrawResultStatus | string | null | undefined): string {
  if (!status) return '—';
  switch (status) {
    case 'CONFIRMED': return 'Confirmé';
    case 'PROVISIONAL': return 'Provisoire';
    case 'OVERRIDDEN': return 'Remplacé';
    case 'ERROR': return 'Erreur';
    case 'APPLIED': return 'Appliqué';
    case 'CORRECTED': return 'Corrigé';
    case 'VOIDED': return 'Annulé';
    case 'PENDING': return 'En attente';
    case 'EXPECTED': return 'Attendu';
    case 'MISSING': return 'Manquant';
    case 'SOURCE_ERROR': return 'Erreur source';
    case 'MANUAL': return 'Manuel';
    case 'REJECTED': return 'Rejeté';
    case 'NOT_DUE': return 'Attendu après fermeture';
    default: return status;
  }
}

export function consoleDrawResultStatusTone(status: ConsoleDrawResultStatus | string | null | undefined): AdminStatusTone {
  switch (status) {
    case 'CONFIRMED':
    case 'APPLIED': return 'success';
    case 'PROVISIONAL':
    case 'CORRECTED':
    case 'OVERRIDDEN': return 'warning';
    case 'ERROR':
    case 'VOIDED':
    case 'MISSING':
    case 'SOURCE_ERROR':
    case 'REJECTED': return 'danger';
    case 'MANUAL': return 'info';
    default: return 'neutral';
  }
}

export function consoleDrawResultQualityLabel(quality: ConsoleDrawResultQuality | string | null | undefined): string {
  if (!quality) return '—';
  switch (quality) {
    case 'COMPLETE': return 'Complet';
    case 'SUSPECT': return 'À vérifier';
    case 'INVALID': return 'Invalide';
    case 'OFFICIAL': return 'Officiel';
    case 'MANUAL': return 'Manuel';
    case 'ESTIMATED': return 'Estimé';
    case 'UNKNOWN': return 'Inconnu';
    default: return quality;
  }
}

export function consoleDrawResultQualityTone(quality: ConsoleDrawResultQuality | string | null | undefined): AdminStatusTone {
  switch (quality) {
    case 'COMPLETE':
    case 'OFFICIAL': return 'success';
    case 'SUSPECT':
    case 'MANUAL':
    case 'ESTIMATED': return 'warning';
    case 'INVALID': return 'danger';
    default: return 'neutral';
  }
}

export function consoleDrawPublicationStatusLabel(status: ConsoleDrawPublicationStatus | string | null | undefined): string {
  if (!status || status === 'NOT_PUBLISHED') return 'Non publié';
  switch (status) {
    case 'PUBLISHED': return 'Publié';
    case 'PROVISIONAL': return 'Provisoire';
    default: return status;
  }
}

export function consoleDrawPublicationStatusTone(status: ConsoleDrawPublicationStatus | string | null | undefined): AdminStatusTone {
  switch (status) {
    case 'PUBLISHED': return 'success';
    case 'PROVISIONAL': return 'warning';
    default: return 'neutral';
  }
}

export function consoleDrawLifecycleActionLabel(action: ConsoleDrawLifecycleDisplayAction): string {
  switch (action) {
    case 'open': return 'Ouvrir la vente';
    case 'close': return 'Fermer la vente';
    case 'lock': return 'Verrouiller';
    case 'unlock': return 'Déverrouiller';
    case 'cancel': return 'Annuler';
    case 'settle': return 'Régler';
    case 'archive': return 'Archiver';
    case 'reschedule': return 'Reprogrammer';
    case 'correct': return 'Corriger résultat';
  }
}

export function consoleDrawLifecycleActionIcon(action: ConsoleDrawLifecycleDisplayAction): string {
  switch (action) {
    case 'open': return 'play_arrow';
    case 'close': return 'stop';
    case 'lock': return 'lock';
    case 'unlock': return 'lock_open';
    case 'cancel': return 'cancel';
    case 'settle': return 'paid';
    case 'archive': return 'inventory_2';
    case 'reschedule': return 'schedule';
    case 'correct': return 'edit';
  }
}
