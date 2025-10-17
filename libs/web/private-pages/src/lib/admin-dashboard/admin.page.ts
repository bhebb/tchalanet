import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@tchl/shared/auth';

@Component({
  standalone: true,
  selector: 'tchl-admin-dashboard-page',
  imports: [CommonModule],
  template: ` <section class="p-4">ADMIN DASHBOARD</section> `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardPage {
  private auth = inject(AuthService);

  readonly layout = computed(() => {
    // const feats = this.featuresSvc.features();
    // return feats ? genLayout(feats) : null;
  });

  constructor() {
    effect(
      async () => {
        // const tenant = this.auth.tenant(); // implémenté chez toi
        // const role = this.auth.bestRole(); // implémenté chez toi
        // await Promise.all([this.featuresSvc.load(tenant, role), this.kpisSvc.load(tenant, role)]);
      },
      { allowSignalWrites: true },
    );
  }
}
