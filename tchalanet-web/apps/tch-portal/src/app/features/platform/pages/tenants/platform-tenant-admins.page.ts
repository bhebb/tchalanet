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

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
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
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      [title]="tenant() ? 'Admins — ' + tenant()!.name : 'Admins du tenant'"
      [description]="tenant() ? tenant()!.code : ''"
    >
      <div actions>
        <a mat-button routerLink="/app/platform/tenants">
          <span class="material-symbols-outlined">arrow_back</span>
          Retour
        </a>
        <a mat-flat-button color="primary" routerLink="new">
          <span class="material-symbols-outlined">person_add</span>
          Ajouter un admin
        </a>
      </div>

      @if (loading()) {
        <div class="loading-state">
          <span class="material-symbols-outlined spin">progress_activity</span>
          Chargement...
        </div>
      } @else if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
          @if (traceId()) {
            <span class="trace-id">ID: {{ traceId() }}</span>
          }
        </div>
      } @else if (admins().length === 0) {
        <tch-admin-empty-state
          icon="person"
          title="Aucun administrateur"
          message="Ajoutez le premier administrateur pour ce tenant."
        />
      } @else {
        <div class="table-container">
          <table mat-table [dataSource]="admins()" class="admin-table">
            <ng-container matColumnDef="email">
              <th mat-header-cell *matHeaderCellDef>Email</th>
              <td mat-cell *matCellDef="let row">{{ row.email }}</td>
            </ng-container>

            <ng-container matColumnDef="displayName">
              <th mat-header-cell *matHeaderCellDef>Nom affiché</th>
              <td mat-cell *matCellDef="let row">{{ row.displayName }}</td>
            </ng-container>

            <ng-container matColumnDef="roleCodes">
              <th mat-header-cell *matHeaderCellDef>Rôles</th>
              <td mat-cell *matCellDef="let row">{{ row.roleCodes.join(', ') }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Statut</th>
              <td mat-cell *matCellDef="let row">{{ row.status }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>
        </div>
      }
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .loading-state {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 2rem;
        color: var(--tch-color-on-surface-variant);
      }

      .spin {
        animation: spin 0.8s linear infinite;
        display: inline-block;
      }

      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
      }

      .trace-id {
        font-size: 0.75rem;
        opacity: 0.7;
      }

      .table-container {
        overflow-x: auto;
      }

      .admin-table {
        width: 100%;
      }
    `,
  ],
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
