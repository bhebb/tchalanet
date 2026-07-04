import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { AdminListSurface, TchErrorPanel, TchLoading, TchStatusBadge } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  AccessRoleView,
  PlatformAccessControlApi,
} from '../data-access/platform-access-control-api.service';

@Component({
  selector: 'tch-platform-roles-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminListSurface,
    AdminSectionCardComponent,
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
      title="Rôles"
      description="Consultez les rôles système et la matrice rôle → permissions supportée par platform.accesscontrol."
    >
      <button actions mat-stroked-button type="button" (click)="loadRoles()">
        <mat-icon>refresh</mat-icon>
        Actualiser
      </button>

      @if (error()) {
        <tch-error-panel [message]="error() ?? 'Opération impossible.'" />
      }

      <div class="platform-access-roles__grid">
        <tch-admin-list-surface
          searchLabel="Rechercher un rôle"
          searchPlaceholder="Code, nom ou description"
          [searchValue]="roleQuery()"
          [showSearchButton]="true"
          searchButtonLabel="Filtrer"
          [filtersDisplay]="'inline'"
          (searchChange)="onRoleSearchFilter($event)"
        >
          <ng-container list-content>
            @if (loadingRoles()) {
              <tch-loading label="Chargement des rôles..." />
            } @else if (!filteredRoles().length) {
              <tch-admin-empty-state icon="group_off" title="Aucun rôle" />
            } @else {
              <div class="platform-access-roles__table-wrap">
                <table mat-table [dataSource]="filteredRoles()" class="platform-access-roles__table">
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

                  <ng-container matColumnDef="scope">
                    <th mat-header-cell *matHeaderCellDef>Scope</th>
                    <td mat-cell *matCellDef="let row">{{ row.tenantId ? 'Tenant' : 'Système' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let row">
                      <button mat-button type="button" (click)="selectRole(row)">
                        <mat-icon>visibility</mat-icon>
                        Voir
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="roleColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: roleColumns"></tr>
                </table>
              </div>
            }
          </ng-container>
        </tch-admin-list-surface>

        <tch-admin-section-card
          title="Matrice rôle → permissions"
          [description]="selectedRole() ? selectedRole()!.code : 'Aucun rôle sélectionné.'"
          icon="rule"
        >
          @if (loadingRolePermissions()) {
            <tch-loading label="Chargement des permissions du rôle..." />
          } @else if (!selectedRole()) {
            <tch-admin-empty-state
              icon="touch_app"
              title="Sélectionnez un rôle"
              message="Sélectionnez un rôle pour voir ses permissions."
            />
          } @else if (!selectedRolePermissionCodes().length) {
            <tch-admin-empty-state icon="key_off" title="Aucune permission liée" />
          } @else {
            <div class="platform-access-roles__badge-list">
              @for (permission of selectedRolePermissionCodes(); track permission) {
                <tch-status-badge status="missing" [label]="permission" />
              }
            </div>
          }
        </tch-admin-section-card>
      </div>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .platform-access-roles__grid {
        display: grid;
        grid-template-columns: minmax(0, 1.15fr) minmax(18rem, 0.85fr);
        gap: 1rem;
      }

      .platform-access-roles__table-wrap {
        overflow-x: auto;
      }

      .platform-access-roles__table {
        width: 100%;
      }

      .platform-access-roles__badge-list {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
      }

      @media (max-width: 960px) {
        .platform-access-roles__grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class PlatformRolesPage implements OnInit {
  private readonly api = inject(PlatformAccessControlApi);

  readonly roleColumns = ['code', 'name', 'scope', 'actions'];
  readonly loadingRoles = signal(false);
  readonly loadingRolePermissions = signal(false);
  readonly error = signal<string | null>(null);

  readonly roles = signal<AccessRoleView[]>([]);
  readonly roleQuery = signal('');
  readonly selectedRole = signal<AccessRoleView | null>(null);
  readonly selectedRolePermissionCodes = signal<string[]>([]);

  readonly filteredRoles = computed(() => {
    const q = this.roleQuery().trim().toLowerCase();
    if (!q) return this.roles();
    return this.roles().filter(role =>
      [role.code, role.name, role.description]
        .filter((value): value is string => Boolean(value))
        .some(value => value.toLowerCase().includes(q)),
    );
  });

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.loadingRoles.set(true);
    this.error.set(null);
    this.api.listRoles().subscribe({
      next: roles => {
        this.roles.set(roles);
        this.loadingRoles.set(false);
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Chargement des rôles impossible.'));
        this.loadingRoles.set(false);
      },
    });
  }

  onRoleSearchFilter(value: string): void {
    this.roleQuery.set(value);
  }

  selectRole(role: AccessRoleView): void {
    this.selectedRole.set(role);
    this.loadingRolePermissions.set(true);
    this.error.set(null);
    this.api.listRolePermissions(role.id).subscribe({
      next: codes => {
        this.selectedRolePermissionCodes.set([...codes].sort((a, b) => a.localeCompare(b)));
        this.loadingRolePermissions.set(false);
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Chargement des permissions du rôle impossible.'));
        this.loadingRolePermissions.set(false);
      },
    });
  }

  private problemTitle(err: unknown, fallback: string): string {
    return (err as { error?: { title?: string } })?.error?.title ?? fallback;
  }
}
