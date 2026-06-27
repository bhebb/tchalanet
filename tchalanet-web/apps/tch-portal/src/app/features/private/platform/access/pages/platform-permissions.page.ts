import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { AdminListSurface, TchErrorPanel, TchLoading, TchStatusBadge } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import {
  AccessPermissionView,
  PlatformAccessControlApi,
} from '../data-access/platform-access-control-api.service';

@Component({
  selector: 'tch-platform-permissions-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminListSurface,
    AdminEmptyStateComponent,
    TchErrorPanel,
    TchLoading,
    TchStatusBadge,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Permissions"
      description="Catalogue système des permissions exposées par platform.accesscontrol. Lecture seule en V1."
    >
      <button actions mat-stroked-button type="button" (click)="load()">
        <mat-icon>refresh</mat-icon>
        Actualiser
      </button>

      @if (error()) {
        <tch-error-panel [message]="error() ?? 'Chargement impossible.'" />
      }

      <tch-admin-list-surface
        searchLabel="Rechercher"
        searchPlaceholder="Code, nom, catégorie ou description"
        [searchValue]="query()"
        resetLabel="Réinitialiser"
        [filtersDisplay]="'inline'"
        [filtersExpanded]="query().length > 0"
        [showSearchButton]="true"
        searchButtonLabel="Filtrer"
        (searchChange)="onSearchFilter($event)"
        (resetFilters)="resetFilter()"
      >
        <ng-container list-content>
          @if (loading()) {
            <tch-loading label="Chargement des permissions..." />
          } @else if (!filteredPermissions().length) {
            <tch-admin-empty-state
              icon="key_off"
              title="Aucune permission"
              message="Aucune permission ne correspond au filtre courant."
            />
          } @else {
            <div class="platform-access-permissions__table-wrap">
              <table mat-table [dataSource]="filteredPermissions()" class="platform-access-permissions__table">
                <ng-container matColumnDef="code">
                  <th mat-header-cell *matHeaderCellDef>Code</th>
                  <td mat-cell *matCellDef="let row">
                    <tch-status-badge status="ready" [label]="row.code" />
                  </td>
                </ng-container>

                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>Nom</th>
                  <td mat-cell *matCellDef="let row">{{ row.name || '—' }}</td>
                </ng-container>

                <ng-container matColumnDef="category">
                  <th mat-header-cell *matHeaderCellDef>Catégorie</th>
                  <td mat-cell *matCellDef="let row">{{ row.category || '—' }}</td>
                </ng-container>

                <ng-container matColumnDef="description">
                  <th mat-header-cell *matHeaderCellDef>Description</th>
                  <td mat-cell *matCellDef="let row">{{ row.description || '—' }}</td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>
          }
        </ng-container>
      </tch-admin-list-surface>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .platform-access-permissions__table-wrap {
        overflow-x: auto;
      }

      .platform-access-permissions__table {
        width: 100%;
      }
    `,
  ],
})
export class PlatformPermissionsPage implements OnInit {
  private readonly api = inject(PlatformAccessControlApi);

  readonly displayedColumns = ['code', 'name', 'category', 'description'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly permissions = signal<AccessPermissionView[]>([]);
  readonly query = signal('');

  readonly filteredPermissions = computed(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.permissions();
    return this.permissions().filter(permission =>
      [permission.code, permission.name, permission.category, permission.description]
        .filter((value): value is string => Boolean(value))
        .some(value => value.toLowerCase().includes(q)),
    );
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listPermissions().subscribe({
      next: permissions => {
        this.permissions.set(permissions);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Chargement des permissions impossible.'));
        this.loading.set(false);
      },
    });
  }

  onSearchFilter(value: string): void {
    this.query.set(value);
  }

  resetFilter(): void {
    this.query.set('');
  }

  private problemTitle(err: unknown, fallback: string): string {
    return (err as { error?: { title?: string } })?.error?.title ?? fallback;
  }
}
