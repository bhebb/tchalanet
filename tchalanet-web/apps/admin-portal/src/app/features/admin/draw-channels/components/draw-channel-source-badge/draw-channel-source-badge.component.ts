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
    switch (a.mode) {
      case 'AUTO':         return 'sync';
      case 'MANUAL':       return 'edit_note';
      case 'UNCONFIGURED': return 'warning';
    }
  });

  readonly label = computed<string>(() => {
    const a = this.acquisition();
    if (a.sourceStatus === 'ERROR') return 'Source en erreur';
    switch (a.mode) {
      case 'AUTO':         return 'Acquisition auto';
      case 'MANUAL':       return 'Saisie manuelle';
      case 'UNCONFIGURED': return 'Mode résultat à définir';
    }
  });

  readonly variant = computed<'ok' | 'manual' | 'warning' | 'error'>(() => {
    const a = this.acquisition();
    if (a.sourceStatus === 'ERROR') return 'error';
    switch (a.mode) {
      case 'AUTO':         return 'ok';
      case 'MANUAL':       return 'manual';
      case 'UNCONFIGURED': return 'warning';
    }
  });
}
