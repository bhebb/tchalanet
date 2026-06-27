import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { forkJoin } from 'rxjs';

import { AdminListSurface, TchErrorPanel, TchLoading, TchStatusBadge } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import {
  AccessPermissionView,
  AccessRoleView,
  PlatformAccessControlApi,
} from '../data-access/platform-access-control-api.service';

type PermissionSort = 'code' | 'name' | 'category' | 'role';

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
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
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
            <div class="platform-access-permissions__filters">
              <mat-form-field appearance="outline">
                <mat-label>Rôle</mat-label>
                <mat-select [value]="roleFilter()" (selectionChange)="setRoleFilter($event.value)">
                  <mat-option value="">Tous les rôles</mat-option>
                  @for (role of roles(); track role.id) {
                    <mat-option [value]="role.id">{{ role.code }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Trier par</mat-label>
                <mat-select [value]="sortBy()" (selectionChange)="setSortBy($event.value)">
                  <mat-option value="code">Code</mat-option>
                  <mat-option value="name">Nom</mat-option>
                  <mat-option value="category">Catégorie</mat-option>
                  <mat-option value="role">Rôle</mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <div class="platform-access-permissions__table-wrap">
              <table mat-table [dataSource]="pagedPermissions()" class="platform-access-permissions__table">
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

                <ng-container matColumnDef="roles">
                  <th mat-header-cell *matHeaderCellDef>Rôles</th>
                  <td mat-cell *matCellDef="let row">
                    <div class="platform-access-permissions__roles">
                      @for (role of rolesForPermission(row.code); track role) {
                        <tch-status-badge status="pending" [label]="role" />
                      } @empty {
                        <span class="muted">—</span>
                      }
                    </div>
                  </td>
                </ng-container>

                <ng-container matColumnDef="description">
                  <th mat-header-cell *matHeaderCellDef>Description</th>
                  <td mat-cell *matCellDef="let row">{{ row.description || '—' }}</td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>

            <div class="platform-access-permissions__pagination">
              <span>{{ filteredPermissions().length }} permission(s) • page {{ page() + 1 }} / {{ totalPages() }}</span>
              <div class="platform-access-permissions__pager-actions">
                <button mat-stroked-button type="button" [disabled]="page() === 0" (click)="previousPage()">
                  Précédent
                </button>
                <button mat-stroked-button type="button" [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">
                  Suivant
                </button>
              </div>
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

      .platform-access-permissions__filters,
      .platform-access-permissions__pagination,
      .platform-access-permissions__pager-actions {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex-wrap: wrap;
      }

      .platform-access-permissions__filters {
        margin-bottom: 0.75rem;
      }

      .platform-access-permissions__filters mat-form-field {
        min-width: 14rem;
      }

      .platform-access-permissions__pagination {
        justify-content: space-between;
        margin-top: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-size: var(--tch-font-size-body-sm, 0.875rem);
      }

      .platform-access-permissions__roles {
        display: flex;
        flex-wrap: wrap;
        gap: 0.25rem;
        min-width: 12rem;
      }

      .muted {
        color: var(--tch-color-on-surface-variant, #46464f);
      }
    `,
  ],
})
export class PlatformPermissionsPage implements OnInit {
  private readonly api = inject(PlatformAccessControlApi);

  readonly displayedColumns = ['code', 'name', 'category', 'roles', 'description'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly permissions = signal<AccessPermissionView[]>([]);
  readonly roles = signal<AccessRoleView[]>([]);
  readonly rolePermissionCodes = signal<Record<string, readonly string[]>>({});
  readonly query = signal('');
  readonly roleFilter = signal('');
  readonly sortBy = signal<PermissionSort>('code');
  readonly page = signal(0);
  readonly pageSize = 25;

  readonly filteredPermissions = computed(() => {
    const q = this.query().trim().toLowerCase();
    const roleId = this.roleFilter();
    const roleCodes = roleId ? new Set(this.rolePermissionCodes()[roleId] ?? []) : null;
    const filtered = this.permissions().filter(permission => {
      if (roleCodes && !roleCodes.has(permission.code)) return false;
      if (!q) return true;
      return [permission.code, permission.name, permission.category, permission.description, this.rolesForPermission(permission.code).join(' ')]
        .filter((value): value is string => Boolean(value))
        .some(value => value.toLowerCase().includes(q));
    });
    return filtered.sort((a, b) => this.sortValue(a).localeCompare(this.sortValue(b)));
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredPermissions().length / this.pageSize)));
  readonly pagedPermissions = computed(() => {
    const start = this.page() * this.pageSize;
    return this.filteredPermissions().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      permissions: this.api.listPermissions(),
      roles: this.api.listRoles(),
    }).subscribe({
      next: ({ permissions, roles }) => {
        this.permissions.set(permissions);
        this.roles.set([...roles].sort((a, b) => a.code.localeCompare(b.code)));
        this.loadRolePermissionMap(roles);
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Chargement des permissions impossible.'));
        this.loading.set(false);
      },
    });
  }

  onSearchFilter(value: string): void {
    this.query.set(value);
    this.page.set(0);
  }

  setRoleFilter(roleId: string): void {
    this.roleFilter.set(roleId);
    this.page.set(0);
  }

  setSortBy(sortBy: PermissionSort): void {
    this.sortBy.set(sortBy);
    this.page.set(0);
  }

  nextPage(): void {
    this.page.update(page => Math.min(page + 1, this.totalPages() - 1));
  }

  previousPage(): void {
    this.page.update(page => Math.max(page - 1, 0));
  }

  resetFilter(): void {
    this.query.set('');
    this.roleFilter.set('');
    this.sortBy.set('code');
    this.page.set(0);
  }

  rolesForPermission(permissionCode: string): string[] {
    const byRole = this.rolePermissionCodes();
    return this.roles()
      .filter(role => (byRole[role.id] ?? []).includes(permissionCode))
      .map(role => role.code);
  }

  private loadRolePermissionMap(roles: readonly AccessRoleView[]): void {
    const requests = Object.fromEntries(roles.map(role => [role.id, this.api.listRolePermissions(role.id)]));
    if (!roles.length) {
      this.rolePermissionCodes.set({});
      this.loading.set(false);
      return;
    }
    forkJoin(requests).subscribe({
      next: rolePermissionCodes => {
        this.rolePermissionCodes.set(rolePermissionCodes);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Chargement des rôles liés impossible.'));
        this.loading.set(false);
      },
    });
  }

  private sortValue(permission: AccessPermissionView): string {
    switch (this.sortBy()) {
      case 'name':
        return permission.name || permission.code;
      case 'category':
        return `${permission.category || ''} ${permission.code}`;
      case 'role':
        return `${this.rolesForPermission(permission.code)[0] || ''} ${permission.code}`;
      case 'code':
      default:
        return permission.code;
    }
  }

  private problemTitle(err: unknown, fallback: string): string {
    return (err as { error?: { title?: string } })?.error?.title ?? fallback;
  }
}
