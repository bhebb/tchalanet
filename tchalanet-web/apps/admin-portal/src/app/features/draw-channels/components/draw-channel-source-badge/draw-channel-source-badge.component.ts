import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DrawResultAcquisitionView } from '../../data-access/admin-draw-channels.models';

@Component({
  selector: 'tch-draw-channel-source-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './draw-channel-source-badge.component.html',
  styleUrls: ['./draw-channel-source-badge.component.scss'],
})
export class DrawChannelSourceBadgeComponent {
  readonly acquisition = input.required<DrawResultAcquisitionView>();

  readonly icon = computed<string>(() => {
    const a = this.acquisition();
    if (a.sourceStatus === 'ERROR') return 'sync_problem';
    if (a.sourceStatus === 'DISABLED') return 'block';
    switch (a.mode) {
      case 'AUTO':         return 'sync';
      case 'MANUAL':       return 'edit_note';
      case 'UNCONFIGURED': return 'warning';
    }
  });

  readonly label = computed<string>(() => {
    const a = this.acquisition();
    if (a.sourceStatus === 'ERROR') return 'Source en erreur';
    if (a.sourceStatus === 'DISABLED') return 'Non disponible';
    switch (a.mode) {
      case 'AUTO':         return 'Acquisition auto';
      case 'MANUAL':       return 'Saisie manuelle';
      case 'UNCONFIGURED': return 'Mode résultat à définir';
    }
  });

  readonly variant = computed<'ok' | 'manual' | 'warning' | 'error' | 'disabled'>(() => {
    const a = this.acquisition();
    if (a.sourceStatus === 'ERROR') return 'error';
    if (a.sourceStatus === 'DISABLED') return 'disabled';
    switch (a.mode) {
      case 'AUTO':         return 'ok';
      case 'MANUAL':       return 'manual';
      case 'UNCONFIGURED': return 'warning';
    }
  });
}
