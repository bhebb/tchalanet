import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { DatePipe } from '@angular/common';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../platform-tenants-api.service';
import { StartTenantAdminAccessDialog } from '../../shared/start-tenant-admin-access-dialog';

@Component({
  selector: 'tch-platform-tenants-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    DatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      [title]="'platform.tenants.title' | translate"
      [description]="'platform.tenants.description' | translate"
    >
      <div actions>
        <a mat-flat-button color="primary" routerLink="../tenants/new">
          <span class="material-symbols-outlined">add</span>
          Créer un tenant
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
      } @else if (items().length === 0) {
        <tch-admin-empty-state
          icon="business"
          [title]="'Aucun tenant'"
          [message]="'Créez le premier tenant pour commencer.'"
        />
      } @else {
        <div class="table-container">
          <table mat-table [dataSource]="items()" class="tenant-table">
            <ng-container matColumnDef="code">
              <th mat-header-cell *matHeaderCellDef>Code</th>
              <td mat-cell *matCellDef="let row">{{ row.code }}</td>
            </ng-container>

            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Nom</th>
              <td mat-cell *matCellDef="let row">{{ row.name }}</td>
            </ng-container>

            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef>Type</th>
              <td mat-cell *matCellDef="let row">{{ row.type }}</td>
            </ng-container>

            <ng-container matColumnDef="timezone">
              <th mat-header-cell *matHeaderCellDef>Fuseau</th>
              <td mat-cell *matCellDef="let row">{{ row.timezone }}</td>
            </ng-container>

            <ng-container matColumnDef="currency">
              <th mat-header-cell *matHeaderCellDef>Devise</th>
              <td mat-cell *matCellDef="let row">{{ row.currency }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Statut</th>
              <td mat-cell *matCellDef="let row">
                <span class="status-badge" [attr.data-status]="row.status">{{ row.status }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let row">
                <button mat-icon-button [matMenuTriggerFor]="rowMenu" [matMenuTriggerData]="{ row }">
                  <span class="material-symbols-outlined">more_vert</span>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>

          <mat-menu #rowMenu="matMenu">
            <ng-template matMenuContent let-row="row">
              <a mat-menu-item [routerLink]="[row.id, 'admins']">
                <span class="material-symbols-outlined">visibility</span>
                Voir
              </a>
              @if (row.status !== 'ARCHIVED') {
                <button mat-menu-item>
                  <span class="material-symbols-outlined">edit</span>
                  Modifier
                </button>
              }
              @if (row.status === 'DRAFT') {
                <button mat-menu-item (click)="activate(row)">
                  <span class="material-symbols-outlined">check_circle</span>
                  Activer
                </button>
              }
              @if (row.status === 'ACTIVE') {
                <button mat-menu-item (click)="suspend(row)">
                  <span class="material-symbols-outlined">pause_circle</span>
                  Suspendre
                </button>
                <button mat-menu-item (click)="openAdminAccess(row)">
                  <span class="material-symbols-outlined">admin_panel_settings</span>
                  Accéder comme admin
                </button>
              }
              @if (row.status === 'SUSPENDED') {
                <button mat-menu-item (click)="reactivate(row)">
                  <span class="material-symbols-outlined">play_circle</span>
                  Réactiver
                </button>
                <button mat-menu-item (click)="openAdminAccess(row)">
                  <span class="material-symbols-outlined">visibility</span>
                  Accéder comme admin (lecture seule)
                </button>
              }
              @if (row.status !== 'ARCHIVED') {
                <button mat-menu-item (click)="archive(row)">
                  <span class="material-symbols-outlined">archive</span>
                  Archiver
                </button>
              }
            </ng-template>
          </mat-menu>
        </div>

        <div class="pagination">
          <button mat-button [disabled]="page() === 0" (click)="prevPage()">
            <span class="material-symbols-outlined">chevron_left</span>
            Précédent
          </button>
          <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
          <button mat-button [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">
            Suivant
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
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
        margin-left: 0.25rem;
      }

      .table-container {
        overflow-x: auto;
      }

      .tenant-table {
        width: 100%;
      }

      .status-badge {
        display: inline-block;
        padding: 0.125rem 0.625rem;
        border-radius: 9999px;
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }

      .status-badge[data-status='ACTIVE'] {
        background: #d4edda;
        color: #155724;
      }

      .status-badge[data-status='SUSPENDED'] {
        background: #fff3cd;
        color: #856404;
      }

      .status-badge[data-status='DRAFT'],
      .status-badge[data-status='ARCHIVED'] {
        background: #e9ecef;
        color: #495057;
      }

      .pagination {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 1rem;
        margin-top: 1rem;
      }
    `,
  ],
})
export class PlatformTenantsPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['code', 'name', 'type', 'timezone', 'currency', 'status', 'actions'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly items = signal<TenantSummaryView[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / 20)));

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listTenants({ page: this.page(), size: 20 }).subscribe({
      next: res => {
        this.items.set(res.items);
        this.total.set(res.total);
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

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update(p => p - 1);
      this.loadPage();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update(p => p + 1);
      this.loadPage();
    }
  }

  activate(tenant: TenantSummaryView): void {
    this.api.activateTenant(tenant.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant ${tenant.code} activé.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors de l\'activation.', 'OK', { duration: 4000 }),
    });
  }

  suspend(tenant: TenantSummaryView): void {
    this.api.suspendTenant(tenant.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant ${tenant.code} suspendu.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors de la suspension.', 'OK', { duration: 4000 }),
    });
  }

  reactivate(tenant: TenantSummaryView): void {
    this.api.reactivateTenant(tenant.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant ${tenant.code} réactivé.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors de la réactivation.', 'OK', { duration: 4000 }),
    });
  }

  archive(tenant: TenantSummaryView): void {
    this.api.archiveTenant(tenant.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant ${tenant.code} archivé.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors de l\'archivage.', 'OK', { duration: 4000 }),
    });
  }

  openAdminAccess(tenant: TenantSummaryView): void {
    this.dialog.open(StartTenantAdminAccessDialog, {
      data: {
        tenantId: tenant.id,
        tenantName: tenant.name,
        tenantCode: tenant.code,
        tenantStatus: tenant.status,
      },
      width: '520px',
    });
  }
}
