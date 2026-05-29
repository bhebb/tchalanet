import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { AuthService } from '@tchl/shared/auth';

@Component({
  standalone: true,
  selector: 'tchl-admin-dashboard-page',
  imports: [CommonModule],
  template: `
    <section class="p-4 space-y-4">
      <h1 class="text-2xl font-semibold">Dashboard (admin)</h1>
      <p class="text-sm text-gray-500">
        Ceci est le tableau de bord d'administration. Utilisé pour tester le routing par rôle.
      </p>

      <div class="rounded border bg-base-100/60 p-4 text-sm space-y-2">
        <div>
          <span class="font-semibold">Rôles actuels :</span>
          <span>{{ roles().length ? roles().join(', ') : 'Aucun rôle (tch.roles vide)' }}</span>
        </div>
      </div>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardPage {
  private auth = inject(AuthService);

  readonly roles = computed(() => this.auth.tch()?.roles ?? []);
}
