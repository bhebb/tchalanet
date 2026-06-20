import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../admin-status-pill.component';

export interface AdminProvisioningHealthItem {
  readonly label: string;
  readonly status: string;
  readonly tone: AdminStatusTone;
}

@Component({
  selector: 'tch-admin-provisioning-health-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminStatusPillComponent, TchErrorPanel, TchLoading],
  templateUrl: './admin-provisioning-health-card.component.html',
  styleUrls: ['./admin-provisioning-health-card.component.scss'],
})
export class AdminProvisioningHealthCardComponent {
  readonly title = input('Santé de la configuration');
  readonly eyebrow = input('Readiness');
  readonly loading = input(false);
  readonly loadingLabel = input('Calcul du profil...');
  readonly error = input<string | null>(null);
  readonly readinessLabel = input.required<string>();
  readonly readinessTone = input<AdminStatusTone>('neutral');
  readonly progress = input<number | null>(null);
  readonly items = input<readonly AdminProvisioningHealthItem[]>([]);
  readonly emptyLabel = input('Aucun élément à afficher.');

  readonly progressValue = computed(() => {
    const value = this.progress();
    if (value === null || value === undefined || Number.isNaN(value)) {
      return null;
    }
    return Math.min(100, Math.max(0, value));
  });
}
