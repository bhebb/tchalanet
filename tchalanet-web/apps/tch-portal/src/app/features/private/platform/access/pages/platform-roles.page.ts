import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Observable } from 'rxjs';

import { AdminListSurface, TchErrorPanel, TchLoading, TchStatusBadge } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../shared/admin-ui/admin-section-card.component';
import {
  AccessPermissionView,
  AccessRoleView,
  AccessUserView,
  EffectivePermissionsView,
  PlatformAccessControlApi,
} from '../data-access/platform-access-control-api.service';

@Component({
  selector: 'tch-platform-roles-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminListSurface,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    TchErrorPanel,
    TchLoading,
    TchStatusBadge,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Rôles et accès utilisateur"
      description="Consultez les rôles, inspectez les permissions effectives d’un admin et appliquez des overrides supportés par platform.accesscontrol."
    >
      <button actions mat-stroked-button type="button" (click)="loadCatalogs()">
        <mat-icon>refresh</mat-icon>
        Actualiser
      </button>

      @if (error()) {
        <tch-error-panel [message]="error() ?? 'Opération impossible.'" />
      }

      <div class="platform-access-roles__catalog-grid">
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
            @if (loadingCatalog()) {
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
          title="Permissions du rôle"
          [description]="selectedRole() ? selectedRole()!.code : 'Aucun rôle sélectionné.'"
          icon="rule"
        >
          @if (loadingRolePermissions()) {
            <tch-loading label="Chargement des permissions du rôle..." />
          } @else if (!selectedRole()) {
            <tch-admin-empty-state
              icon="touch_app"
              title="Sélectionnez un rôle"
              message="La matrice rôle → permissions s’affichera ici."
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

      <tch-admin-section-card
        title="Accès utilisateur"
        description="Rechercher un admin, inspecter ses permissions effectives, puis ajouter/retirer un rôle ou un override."
        icon="manage_accounts"
      >
        <tch-admin-list-surface
          searchLabel="Rechercher un utilisateur"
          searchPlaceholder="Email, nom ou tenant"
          [searchValue]="userQuery()"
          [showSearchButton]="true"
          searchButtonLabel="Rechercher"
          [filtersDisplay]="'inline'"
          (searchChange)="onUserSearchFilter($event)"
        >
          <ng-container list-content>
            @if (loadingUsers()) {
              <tch-loading label="Recherche des utilisateurs..." />
            } @else if (!users().length) {
              <tch-admin-empty-state
                icon="person_search"
                title="Aucun utilisateur"
                message="Lancez une recherche pour sélectionner un admin."
              />
            } @else {
              <div class="platform-access-roles__table-wrap">
                <table mat-table [dataSource]="users()" class="platform-access-roles__table">
                  <ng-container matColumnDef="user">
                    <th mat-header-cell *matHeaderCellDef>Utilisateur</th>
                    <td mat-cell *matCellDef="let row">
                      <strong>{{ row.displayName || row.email || row.username || row.id }}</strong>
                      <span class="muted">{{ row.email || '—' }}</span>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="tenant">
                    <th mat-header-cell *matHeaderCellDef>Tenant</th>
                    <td mat-cell *matCellDef="let row">{{ row.tenantCode || row.tenantName || '—' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="role">
                    <th mat-header-cell *matHeaderCellDef>Rôle</th>
                    <td mat-cell *matCellDef="let row">
                      @if (row.role) {
                        <tch-status-badge status="pending" [label]="row.role" />
                      } @else {
                        —
                      }
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Statut</th>
                    <td mat-cell *matCellDef="let row">{{ row.status }}</td>
                  </ng-container>

                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Actions</th>
                    <td mat-cell *matCellDef="let row">
                      <button mat-button type="button" (click)="selectUser(row)">
                        <mat-icon>admin_panel_settings</mat-icon>
                        Gérer
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="userColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: userColumns"></tr>
                </table>
              </div>
            }
          </ng-container>
        </tch-admin-list-surface>

        @if (selectedUser()) {
          <div class="platform-access-roles__user-panel">
            <header class="platform-access-roles__user-panel-header">
              <div>
                <h3>{{ selectedUser()!.displayName || selectedUser()!.email || selectedUser()!.id }}</h3>
                <p>{{ selectedUser()!.tenantCode || selectedUser()!.tenantName || 'Contexte courant' }}</p>
              </div>
              <button mat-stroked-button type="button" (click)="loadEffectivePermissions()">
                <mat-icon>refresh</mat-icon>
                Recharger
              </button>
            </header>

            @if (loadingEffective()) {
              <tch-loading label="Chargement des permissions effectives..." />
            } @else if (effective()) {
              <div class="platform-access-roles__effective-grid">
                <div>
                  <h4>Rôles actifs</h4>
                  <div class="platform-access-roles__badge-list">
                    @for (roleId of effective()!.roleIds; track roleId) {
                      <tch-status-badge status="pending" [label]="roleLabel(roleId)" />
                    } @empty {
                      <span class="muted">Aucun rôle actif.</span>
                    }
                  </div>
                </div>
                <div>
                  <h4>Permissions effectives</h4>
                  <div class="platform-access-roles__badge-list platform-access-roles__badge-list--tall">
                    @for (permission of sortedEffectivePermissions(); track permission) {
                      <tch-status-badge status="missing" [label]="permission" />
                    } @empty {
                      <span class="muted">Aucune permission effective.</span>
                    }
                  </div>
                </div>
              </div>

              <form class="platform-access-roles__actions-grid" [formGroup]="actionForm">
                <mat-form-field appearance="outline">
                  <mat-label>Rôle</mat-label>
                  <mat-select formControlName="roleCode">
                    @for (role of roles(); track role.id) {
                      <mat-option [value]="role.code">{{ role.code }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <div class="platform-access-roles__button-row">
                  <button mat-flat-button color="primary" type="button" [disabled]="saving()" (click)="assignRole()">
                    Ajouter rôle
                  </button>
                  <button mat-stroked-button type="button" [disabled]="saving()" (click)="removeRole()">
                    Retirer rôle
                  </button>
                </div>

                <mat-form-field appearance="outline">
                  <mat-label>Permission</mat-label>
                  <mat-select formControlName="permissionCode">
                    @for (permission of permissions(); track permission.code) {
                      <mat-option [value]="permission.code">{{ permission.code }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>Raison</mat-label>
                  <input matInput formControlName="reason" placeholder="Justification audit" />
                </mat-form-field>
                <div class="platform-access-roles__button-row">
                  <button mat-flat-button color="primary" type="button" [disabled]="saving()" (click)="grantPermission()">
                    Ajouter permission
                  </button>
                  <button mat-stroked-button type="button" [disabled]="saving()" (click)="denyPermission()">
                    Retirer permission
                  </button>
                  <button mat-stroked-button type="button" [disabled]="saving()" (click)="removeOverride()">
                    Retirer override
                  </button>
                </div>
              </form>
            }
          </div>
        }
      </tch-admin-section-card>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .platform-access-roles__catalog-grid {
        display: grid;
        grid-template-columns: minmax(0, 1.15fr) minmax(18rem, 0.85fr);
        gap: 1rem;
        margin-bottom: 1rem;
      }

      .platform-access-roles__actions-grid {
        display: grid;
        grid-template-columns: minmax(16rem, 1fr) auto;
        gap: 0.75rem;
        align-items: flex-start;
        margin-bottom: 1rem;
      }

      .platform-access-roles__actions-grid {
        grid-template-columns: minmax(14rem, 1fr) minmax(14rem, 1fr) auto;
        margin-top: 1rem;
      }

      .platform-access-roles__table-wrap {
        overflow-x: auto;
      }

      .platform-access-roles__table {
        width: 100%;
      }

      .muted {
        display: block;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
      }

      .platform-access-roles__badge-list {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
      }

      .platform-access-roles__badge-list--tall {
        max-height: 16rem;
        overflow: auto;
      }

      .platform-access-roles__user-panel {
        margin-top: 1.25rem;
        padding-top: 1.25rem;
        border-top: 1px solid var(--tch-color-outline-variant, #c8c5d0);
      }

      .platform-access-roles__user-panel-header,
      .platform-access-roles__button-row {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 0.75rem;
      }

      .platform-access-roles__button-row {
        justify-content: flex-start;
        flex-wrap: wrap;
      }

      .platform-access-roles__user-panel h3,
      .platform-access-roles__user-panel h4,
      .platform-access-roles__user-panel p {
        margin: 0;
      }

      .platform-access-roles__user-panel h4 {
        margin-bottom: 0.5rem;
      }

      .platform-access-roles__effective-grid {
        display: grid;
        grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
        gap: 1rem;
        margin-top: 1rem;
      }

      @media (max-width: 960px) {
        .platform-access-roles__catalog-grid,
        .platform-access-roles__actions-grid,
        .platform-access-roles__effective-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class PlatformRolesPage implements OnInit {
  private readonly api = inject(PlatformAccessControlApi);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly roleColumns = ['code', 'name', 'scope', 'actions'];
  readonly userColumns = ['user', 'tenant', 'role', 'status', 'actions'];
  readonly loadingCatalog = signal(false);
  readonly loadingRolePermissions = signal(false);
  readonly loadingUsers = signal(false);
  readonly loadingEffective = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly roles = signal<AccessRoleView[]>([]);
  readonly permissions = signal<AccessPermissionView[]>([]);
  readonly roleQuery = signal('');
  readonly selectedRole = signal<AccessRoleView | null>(null);
  readonly selectedRolePermissionCodes = signal<string[]>([]);
  readonly userQuery = signal('');
  readonly users = signal<AccessUserView[]>([]);
  readonly selectedUser = signal<AccessUserView | null>(null);
  readonly effective = signal<EffectivePermissionsView | null>(null);

  readonly actionForm = this.fb.nonNullable.group({
    roleCode: [''],
    permissionCode: [''],
    reason: [''],
  });

  readonly filteredRoles = computed(() => {
    const q = this.roleQuery().trim().toLowerCase();
    if (!q) return this.roles();
    return this.roles().filter(role =>
      [role.code, role.name, role.description]
        .filter((value): value is string => Boolean(value))
        .some(value => value.toLowerCase().includes(q)),
    );
  });

  readonly sortedEffectivePermissions = computed(() =>
    [...(this.effective()?.permissionCodes ?? [])].sort((a, b) => a.localeCompare(b)),
  );

  ngOnInit(): void {
    this.loadCatalogs();
    this.searchUsers();
  }

  loadCatalogs(): void {
    this.loadingCatalog.set(true);
    this.error.set(null);
    this.api.listRoles().subscribe({
      next: roles => {
        this.roles.set(roles);
        this.actionForm.patchValue({ roleCode: roles[0]?.code ?? this.actionForm.controls.roleCode.value });
        this.api.listPermissions().subscribe({
          next: permissions => {
            this.permissions.set(permissions);
            this.actionForm.patchValue({
              permissionCode: permissions[0]?.code ?? this.actionForm.controls.permissionCode.value,
            });
            this.loadingCatalog.set(false);
          },
          error: err => {
            this.error.set(this.problemTitle(err, 'Chargement des permissions impossible.'));
            this.loadingCatalog.set(false);
          },
        });
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Chargement des rôles impossible.'));
        this.loadingCatalog.set(false);
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

  searchUsers(): void {
    this.loadingUsers.set(true);
    this.error.set(null);
    this.api.searchUsers({ q: this.userQuery(), size: 10 }).subscribe({
      next: page => {
        this.users.set([...page.items]);
        this.loadingUsers.set(false);
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Recherche utilisateur impossible.'));
        this.loadingUsers.set(false);
      },
    });
  }

  onUserSearchFilter(value: string): void {
    this.userQuery.set(value);
    this.searchUsers();
  }

  selectUser(user: AccessUserView): void {
    this.selectedUser.set(user);
    this.loadEffectivePermissions();
  }

  loadEffectivePermissions(): void {
    const user = this.selectedUser();
    if (!user) return;
    this.loadingEffective.set(true);
    this.error.set(null);
    this.api.getEffectivePermissions(user.id).subscribe({
      next: effective => {
        this.effective.set(effective);
        this.loadingEffective.set(false);
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Chargement des permissions effectives impossible.'));
        this.loadingEffective.set(false);
      },
    });
  }

  assignRole(): void {
    const user = this.selectedUser();
    const roleCode = this.actionForm.controls.roleCode.value;
    if (!user || !roleCode) return;
    this.save(() => this.api.assignRole(user.id, roleCode), 'Rôle ajouté.');
  }

  removeRole(): void {
    const user = this.selectedUser();
    const roleCode = this.actionForm.controls.roleCode.value;
    if (!user || !roleCode) return;
    this.save(() => this.api.removeRole(user.id, roleCode), 'Rôle retiré.');
  }

  grantPermission(): void {
    const user = this.selectedUser();
    const permissionCode = this.actionForm.controls.permissionCode.value;
    if (!user || !permissionCode) return;
    this.save(
      () => this.api.grantPermission(user.id, permissionCode, this.actionForm.controls.reason.value || null),
      'Permission ajoutée.',
    );
  }

  denyPermission(): void {
    const user = this.selectedUser();
    const permissionCode = this.actionForm.controls.permissionCode.value;
    if (!user || !permissionCode) return;
    this.save(
      () => this.api.denyPermission(user.id, permissionCode, this.actionForm.controls.reason.value || null),
      'Permission retirée par override DENY.',
    );
  }

  removeOverride(): void {
    const user = this.selectedUser();
    const permissionCode = this.actionForm.controls.permissionCode.value;
    if (!user || !permissionCode) return;
    this.save(() => this.api.removePermissionOverride(user.id, permissionCode), 'Override retiré.');
  }

  roleLabel(roleId: string): string {
    return this.roles().find(role => role.id === roleId)?.code ?? roleId;
  }

  private save(operation: () => Observable<void>, message: string): void {
    this.saving.set(true);
    this.error.set(null);
    operation().subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open(message, 'OK', { duration: 3000 });
        this.loadEffectivePermissions();
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Action impossible.'));
        this.saving.set(false);
      },
    });
  }

  private problemTitle(err: unknown, fallback: string): string {
    return (err as { error?: { title?: string } })?.error?.title ?? fallback;
  }
}
