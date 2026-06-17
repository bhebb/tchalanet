import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { switchMap, forkJoin } from 'rxjs';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import {
  PlatformTenantsApi,
  TenantAdminView,
  TenantSummaryView,
} from '../../platform-tenants-api.service';

@Component({
  selector: 'tch-platform-tenant-admins-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminCrudShellComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-tenant-admins.page.html',
  styleUrls: ['./platform-tenant-admins.page.scss'],
})
export class PlatformTenantAdminsPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly route = inject(ActivatedRoute);

  readonly displayedColumns = ['email', 'displayName', 'roleCodes', 'status'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly tenant = signal<TenantSummaryView | null>(null);
  readonly admins = signal<TenantAdminView[]>([]);

  ngOnInit(): void {
    const tenantId = this.route.snapshot.paramMap.get('tenantId')!;
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      tenant: this.api.getTenant(tenantId),
      admins: this.api.listTenantAdmins(tenantId),
    }).subscribe({
      next: ({ tenant, admins }) => {
        this.tenant.set(tenant);
        this.admins.set(admins);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })
          ?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        this.loading.set(false);
      },
    });
  }
}
