import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { PlatformAdminApi, PlatformSuperAdminView } from '../../../tenants/data-access/platform-admin-api.service';
import { IdentityUserCrudApi } from '../../../shared/identity-user-crud-api.service';

@Component({
  selector: 'tch-platform-admin-users-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
  ],
  templateUrl: './platform-admin-users.page.html',
  styleUrls: ['./platform-admin-users.page.scss'],
})
export class PlatformAdminUsersPage implements OnInit {
  private readonly api = inject(PlatformAdminApi);
  private readonly identityApi = inject(IdentityUserCrudApi);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['email', 'displayName', 'status', 'assignedAt', 'actions'];
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly superAdmins = signal<PlatformSuperAdminView[]>([]);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    displayName: ['', Validators.required],
    phoneNumber: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  submit(): void {
    if (this.form.invalid || this.saving()) return;
    const v = this.form.value;
    this.saving.set(true);
    this.error.set(null);
    this.api
      .createSuperAdmin({
        email: v.email!,
        displayName: v.displayName!,
        phoneNumber: v.phoneNumber || null,
      })
      .subscribe({
        next: created => {
          this.superAdmins.set([created, ...this.superAdmins()]);
          this.form.reset({ email: '', displayName: '', phoneNumber: '' });
          this.saving.set(false);
          this.snackBar.open('Super admin créé.', 'OK', { duration: 3000 });
        },
        error: err => {
          this.error.set(this.problemTitle(err, 'Création impossible.'));
          this.saving.set(false);
        },
      });
  }

  revoke(row: PlatformSuperAdminView): void {
    this.api.revokeSuperAdmin(row.id).subscribe({
      next: () => {
        this.superAdmins.set(this.superAdmins().filter(item => item.id !== row.id));
        this.snackBar.open('Accès super admin retiré.', 'OK', { duration: 3000 });
      },
      error: err => this.error.set(this.problemTitle(err, 'Retrait impossible.')),
    });
  }

  suspend(row: PlatformSuperAdminView): void {
    this.identityApi.suspend(row.id).subscribe({
      next: () => this.updateStatus(row.id, 'SUSPENDED', 'Super admin suspendu.'),
      error: err => this.error.set(this.problemTitle(err, 'Suspension impossible.')),
    });
  }

  reactivate(row: PlatformSuperAdminView): void {
    this.identityApi.activate(row.id).subscribe({
      next: () => this.updateStatus(row.id, 'ACTIVE', 'Super admin réactivé.'),
      error: err => this.error.set(this.problemTitle(err, 'Réactivation impossible.')),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listSuperAdmins().subscribe({
      next: rows => {
        this.superAdmins.set(rows);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.problemTitle(err, 'Chargement impossible.'));
        this.loading.set(false);
      },
    });
  }

  private problemTitle(err: unknown, fallback: string): string {
    return (err as { error?: { title?: string } })?.error?.title ?? fallback;
  }

  private updateStatus(userId: string, status: string, message: string): void {
    this.superAdmins.set(
      this.superAdmins().map(row => (row.id === userId ? { ...row, status } : row)),
    );
    this.snackBar.open(message, 'OK', { duration: 3000 });
  }
}
