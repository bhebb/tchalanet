import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { forkJoin } from 'rxjs';

import { AdminListSurface, TchErrorPanel, TchLoading, TchStatusBadge } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
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
  templateUrl: './platform-permissions.page.html',
  styleUrl: './platform-permissions.page.scss',
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
