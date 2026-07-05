import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { form, maxLength, pattern, required, submit } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  ConsoleActorIdentity,
  ConsoleActorRowComponent,
  ConsolePersonIdentityFormSectionComponent,
} from '@tch/web/console';
import { PlatformSuperAdminsApi, PlatformSuperAdminView } from '../../data-access/platform-super-admins-api.service';
import { IdentityUserCrudApi } from '../../../shared/data-access/identity-user-crud-api.service';
import { platformSuperAdminActorIdentity } from '../../../shared/console-actor-adapters';

interface SuperAdminCreateFormModel {
  readonly email: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly phoneNumber: string;
}

const EMPTY_SUPER_ADMIN_CREATE_FORM: SuperAdminCreateFormModel = {
  email: '',
  firstName: '',
  lastName: '',
  phoneNumber: '',
};

@Component({
  selector: 'tch-platform-super-admins-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    DatePipe,
    AdminPageShellComponent,
    ConsoleActorRowComponent,
    ConsolePersonIdentityFormSectionComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-super-admins.page.html',
  styleUrls: ['./platform-super-admins.page.scss'],
})
export class PlatformSuperAdminsPage implements OnInit {
  private readonly api = inject(PlatformSuperAdminsApi);
  private readonly identityApi = inject(IdentityUserCrudApi);
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['actor', 'assignedAt', 'actions'];
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly superAdmins = signal<PlatformSuperAdminView[]>([]);
  readonly model = signal<SuperAdminCreateFormModel>(EMPTY_SUPER_ADMIN_CREATE_FORM);
  readonly form = form(this.model, path => {
    required(path.email, { message: 'common.validation.required' });
    pattern(path.email, /^[^@\s]+@[^@\s]+\.[^@\s]+$/, { message: 'common.validation.email' });
    required(path.firstName, { message: 'common.validation.required' });
    maxLength(path.firstName, 100, { message: 'common.validation.invalid' });
    maxLength(path.lastName, 100, { message: 'common.validation.invalid' });
    maxLength(path.phoneNumber, 32, { message: 'common.validation.invalid' });
  });

  ngOnInit(): void {
    this.load();
  }

  submit(): void {
    if (this.saving()) return;
    submit(this.form, async () => {
      const v = this.model();
      const displayName = [v.firstName, v.lastName].filter(Boolean).join(' ').trim();

      this.saving.set(true);
      this.error.set(null);
      try {
        const created = await firstValueFrom(
          this.api.createSuperAdmin({
            email: v.email,
            displayName,
            phoneNumber: v.phoneNumber || null,
          }, { suppressShellFeedback: true }),
        );
        this.superAdmins.set([created, ...this.superAdmins()]);
        this.model.set(EMPTY_SUPER_ADMIN_CREATE_FORM);
      } catch (err) {
        this.error.set(this.errorViewModel(err, 'platform.superAdmins.create'));
      } finally {
        this.saving.set(false);
      }
    });
  }

  revoke(row: PlatformSuperAdminView): void {
    this.error.set(null);
    this.api.revokeSuperAdmin(row.id, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.superAdmins.set(this.superAdmins().filter(item => item.id !== row.id));
      },
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.revoke')),
    });
  }

  suspend(row: PlatformSuperAdminView): void {
    this.error.set(null);
    this.identityApi.suspend(row.id, { suppressShellFeedback: true }).subscribe({
      next: () => this.updateStatus(row.id, 'SUSPENDED'),
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.suspend')),
    });
  }

  reactivate(row: PlatformSuperAdminView): void {
    this.error.set(null);
    this.identityApi.activate(row.id, { suppressShellFeedback: true }).subscribe({
      next: () => this.updateStatus(row.id, 'ACTIVE'),
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.reactivate')),
    });
  }

  actorIdentity(row: PlatformSuperAdminView): ConsoleActorIdentity {
    return platformSuperAdminActorIdentity(row);
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listSuperAdmins({ suppressShellFeedback: true }).subscribe({
      next: rows => {
        this.superAdmins.set(rows);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.errorViewModel(err, 'platform.superAdmins.list'));
        this.loading.set(false);
      },
    });
  }

  private updateStatus(userId: string, status: string): void {
    this.superAdmins.set(
      this.superAdmins().map(row => (row.id === userId ? { ...row, status } : row)),
    );
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
