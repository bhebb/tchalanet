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
  templateUrl: './platform-roles.page.html',
  styleUrl: './platform-roles.page.scss',
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
