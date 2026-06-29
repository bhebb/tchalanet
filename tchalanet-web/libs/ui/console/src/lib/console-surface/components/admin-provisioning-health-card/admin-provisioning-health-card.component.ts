import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { TchLoading, TchSectionError, TchSectionErrorSeverity } from '@tch/ui/components';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../admin-status-pill.component';

export interface AdminProvisioningHealthItem {
  readonly label: string;
  readonly status: string;
  readonly tone: AdminStatusTone;
}

export interface AdminProvisioningHealthError {
  readonly title: string;
  readonly message: string;
  readonly severity?: TchSectionErrorSeverity;
}

@Component({
  selector: 'tch-admin-provisioning-health-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminStatusPillComponent, TchLoading, TchSectionError],
  templateUrl: './admin-provisioning-health-card.component.html',
  styleUrls: ['./admin-provisioning-health-card.component.scss'],
})
export class AdminProvisioningHealthCardComponent {
  readonly title = input('Santé de la configuration');
  readonly eyebrow = input('Readiness');
  readonly loading = input(false);
  readonly loadingLabel = input('Calcul du profil...');
  readonly error = input<string | null>(null);
  readonly errorTitle = input<string | null>(null);
  readonly errorMessage = input<string | null>(null);
  readonly errorSeverity = input<TchSectionErrorSeverity>('warn');
  readonly readinessLabel = input.required<string>();
  readonly readinessTone = input<AdminStatusTone>('neutral');
  readonly progress = input<number | null>(null);
  readonly items = input<readonly AdminProvisioningHealthItem[]>([]);
  readonly emptyLabel = input('Aucun élément à afficher.');
  /** Collapse the list past this many items in the (narrow) right rail. 0 disables collapsing. */
  readonly collapsibleAfter = input(5);
  readonly showAllLabel = input('Voir tous les détails');
  readonly showLessLabel = input('Réduire');

  protected readonly expanded = signal(false);

  readonly progressValue = computed(() => {
    const value = this.progress();
    if (value === null || value === undefined || Number.isNaN(value)) {
      return null;
    }
    return Math.min(100, Math.max(0, value));
  });

  /** Items shown now: all when expanded or under the limit, else the first N. */
  readonly visibleItems = computed(() => {
    const items = this.items();
    const limit = this.collapsibleAfter();
    if (limit <= 0 || this.expanded() || items.length <= limit) return items;
    return items.slice(0, limit);
  });

  readonly hiddenCount = computed(() => {
    const limit = this.collapsibleAfter();
    if (limit <= 0) return 0;
    return Math.max(0, this.items().length - limit);
  });

  readonly resolvedErrorTitle = computed(() => this.errorTitle() ?? this.error());
  readonly resolvedErrorMessage = computed(() => this.errorMessage() ?? '');

  toggle(): void {
    this.expanded.update(value => !value);
  }
}
