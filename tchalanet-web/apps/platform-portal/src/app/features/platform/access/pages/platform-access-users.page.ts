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
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  AccessPermissionView,
  AccessRoleView,
  AccessUserView,
  EffectivePermissionsView,
  PlatformAccessControlApi,
} from '../data-access/platform-access-control-api.service';

const HIDDEN_ASSIGNABLE_ROLES = new Set(['CASHIER', 'SELLER_TERMINAL']);

@Component({
  selector: 'tch-platform-access-users-page',
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
      title="Accès utilisateurs"
      description="Rechercher un admin tenant, inspecter ses permissions effectives, puis ajouter ou retirer un rôle ou un override."
    >
      <button actions mat-stroked-button type="button" (click)="reload()">
        <mat-icon>refresh</mat-icon>
        Recharger
      </button>

      @if (error()) {
        <tch-error-panel [message]="error() ?? 'Opération impossible.'" />
      }

      <tch-admin-section-card
        title="Admins tenant"
        description="Les seller terminals ne sont pas listés ici."
        icon="manage_accounts"
      >
        <tch-admin-list-surface
          searchLabel="Rechercher un admin tenant"
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
                title="Aucun admin"
                message="Lancez une recherche pour sélectionner un admin tenant."
              />
            } @else {
              <div class="platform-access-users__table-wrap">
                <table mat-table [dataSource]="users()" class="platform-access-users__table">
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
                        Gérer accès
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="userColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: userColumns"></tr>
                </table>
              </div>

              <div class="platform-access-users__pagination">
                <span>
                  {{ userTotalElements() }} admin(s) • page {{ userPage() + 1 }} / {{ userTotalPages() }}
                </span>
                <div class="platform-access-users__button-row">
                  <button mat-stroked-button type="button" [disabled]="userPage() === 0" (click)="previousUserPage()">
                    Précédent
                  </button>
                  <button
                    mat-stroked-button
                    type="button"
                    [disabled]="userPage() + 1 >= userTotalPages()"
                    (click)="nextUserPage()"
                  >
                    Suivant
                  </button>
                </div>
              </div>
            }
          </ng-container>
        </tch-admin-list-surface>
      </tch-admin-section-card>

      @if (selectedUser()) {
        <tch-admin-section-card title="Droits effectifs" icon="verified_user">
          <header class="platform-access-users__panel-header">
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
            <div class="platform-access-users__effective-grid">
              <div>
                <h4>Rôles actifs</h4>
                <div class="platform-access-users__badge-list">
                  @for (roleId of effective()!.roleIds; track roleId) {
                    <tch-status-badge status="pending" [label]="roleLabel(roleId)" />
                  } @empty {
                    <span class="muted">Aucun rôle actif.</span>
                  }
                </div>
              </div>
              <div>
                <h4>Permissions effectives</h4>
                <div class="platform-access-users__badge-list platform-access-users__badge-list--tall">
                  @for (permission of sortedEffectivePermissions(); track permission) {
                    <tch-status-badge status="missing" [label]="permission" />
                  } @empty {
                    <span class="muted">Aucune permission effective.</span>
                  }
                </div>
              </div>
            </div>

            <form class="platform-access-users__actions-grid" [formGroup]="actionForm">
              <mat-form-field appearance="outline">
                <mat-label>Rôle</mat-label>
                <mat-select formControlName="roleCode">
                  @for (role of assignableRoles(); track role.id) {
                    <mat-option [value]="role.code">{{ role.code }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <div class="platform-access-users__button-row">
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
              <div class="platform-access-users__button-row">
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
        </tch-admin-section-card>
      }
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .platform-access-users__table-wrap {
        overflow-x: auto;
      }

      .platform-access-users__table {
        width: 100%;
      }

      .muted {
        display: block;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
      }

      .platform-access-users__pagination,
      .platform-access-users__panel-header,
      .platform-access-users__button-row {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 0.75rem;
      }

      .platform-access-users__button-row {
        justify-content: flex-start;
        flex-wrap: wrap;
      }

      .platform-access-users__pagination {
        align-items: center;
        flex-wrap: wrap;
        margin-top: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-size: var(--tch-font-size-body-sm, 0.875rem);
      }

      .platform-access-users__panel-header h3,
      .platform-access-users__panel-header p,
      .platform-access-users__effective-grid h4 {
        margin: 0;
      }

      .platform-access-users__effective-grid h4 {
        margin-bottom: 0.5rem;
      }

      .platform-access-users__effective-grid {
        display: grid;
        grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
        gap: 1rem;
        margin-top: 1rem;
      }

      .platform-access-users__badge-list {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
      }

      .platform-access-users__badge-list--tall {
        max-height: 16rem;
        overflow: auto;
      }

      .platform-access-users__actions-grid {
        display: grid;
        grid-template-columns: minmax(14rem, 1fr) minmax(14rem, 1fr) auto;
        gap: 0.75rem;
        align-items: flex-start;
        margin-top: 1rem;
      }

      @media (max-width: 960px) {
        .platform-access-users__actions-grid,
        .platform-access-users__effective-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class PlatformAccessUsersPage implements OnInit {
  private readonly api = inject(PlatformAccessControlApi);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly userColumns = ['user', 'tenant', 'role', 'status', 'actions'];
  readonly loadingCatalog = signal(false);
  readonly loadingUsers = signal(false);
  readonly loadingEffective = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly roles = signal<AccessRoleView[]>([]);
  readonly permissions = signal<AccessPermissionView[]>([]);
  readonly userQuery = signal('');
  readonly users = signal<AccessUserView[]>([]);
  readonly userPage = signal(0);
  readonly userTotalElements = signal(0);
  readonly userTotalPages = signal(1);
  readonly selectedUser = signal<AccessUserView | null>(null);
  readonly effective = signal<EffectivePermissionsView | null>(null);

  readonly actionForm = this.fb.nonNullable.group({
    roleCode: [''],
    permissionCode: [''],
    reason: [''],
  });

  readonly assignableRoles = computed(() =>
    this.roles().filter(role => !HIDDEN_ASSIGNABLE_ROLES.has(role.code)),
  );

  readonly sortedEffectivePermissions = computed(() =>
    [...(this.effective()?.permissionCodes ?? [])].sort((a, b) => a.localeCompare(b)),
  );

  ngOnInit(): void {
    this.loadCatalogs();
    this.searchUsers();
  }

  reload(): void {
    this.loadCatalogs();
    this.searchUsers(this.userPage());
    this.loadEffectivePermissions();
  }

  loadCatalogs(): void {
    this.loadingCatalog.set(true);
    this.error.set(null);
    this.api.listRoles().subscribe({
      next: roles => {
        this.roles.set(roles);
        this.actionForm.patchValue({
          roleCode: this.assignableRoles()[0]?.code ?? this.actionForm.controls.roleCode.value,
        });
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

  searchUsers(page = this.userPage()): void {
    this.loadingUsers.set(true);
    this.error.set(null);
    this.api.searchUsers({ q: this.userQuery(), page, size: 10 }).subscribe({
      next: result => {
        this.users.set([...result.items]);
        this.userPage.set(result.page);
        this.userTotalElements.set(result.totalElements);
        this.userTotalPages.set(Math.max(1, result.totalPages));
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
    this.selectedUser.set(null);
    this.effective.set(null);
    this.searchUsers(0);
  }

  nextUserPage(): void {
    this.searchUsers(Math.min(this.userPage() + 1, this.userTotalPages() - 1));
  }

  previousUserPage(): void {
    this.searchUsers(Math.max(this.userPage() - 1, 0));
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
    this.api.getEffectivePermissions(user.id, user.tenantId).subscribe({
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
    this.save(() => this.api.assignRole(user.id, roleCode, user.tenantId), 'Rôle ajouté.');
  }

  removeRole(): void {
    const user = this.selectedUser();
    const roleCode = this.actionForm.controls.roleCode.value;
    if (!user || !roleCode) return;
    this.save(() => this.api.removeRole(user.id, roleCode, user.tenantId), 'Rôle retiré.');
  }

  grantPermission(): void {
    const user = this.selectedUser();
    const permissionCode = this.actionForm.controls.permissionCode.value;
    if (!user || !permissionCode) return;
    this.save(
      () =>
        this.api.grantPermission(
          user.id,
          permissionCode,
          this.actionForm.controls.reason.value || null,
          user.tenantId,
        ),
      'Permission ajoutée.',
    );
  }

  denyPermission(): void {
    const user = this.selectedUser();
    const permissionCode = this.actionForm.controls.permissionCode.value;
    if (!user || !permissionCode) return;
    this.save(
      () =>
        this.api.denyPermission(
          user.id,
          permissionCode,
          this.actionForm.controls.reason.value || null,
          user.tenantId,
        ),
      'Permission retirée par override DENY.',
    );
  }

  removeOverride(): void {
    const user = this.selectedUser();
    const permissionCode = this.actionForm.controls.permissionCode.value;
    if (!user || !permissionCode) return;
    this.save(
      () => this.api.removePermissionOverride(user.id, permissionCode, user.tenantId),
      'Override retiré.',
    );
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
