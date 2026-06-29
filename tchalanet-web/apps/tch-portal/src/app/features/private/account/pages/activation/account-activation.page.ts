import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthSessionService, PrivateBootstrapStore } from '@tch/core/auth';
import { TchErrorPanel } from '@tch/ui/components';
import { AdminDetailLayoutComponent } from '../../../shared/admin-ui/components/admin-detail-layout/admin-detail-layout.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../shared/admin-ui/admin-section-card.component';
import { AccountActivationApi } from '../../data-access/account-activation-api.service';

type ActivationState = 'loading' | 'ready' | 'invalid' | 'submitting' | 'submitError' | 'success';

@Component({
  selector: 'tch-account-activation-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AdminPageShellComponent,
    AdminDetailLayoutComponent,
    AdminSectionCardComponent,
    TchErrorPanel,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './account-activation.page.html',
  styleUrls: ['./account-activation.page.scss'],
})
export class AccountActivationPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthSessionService);
  private readonly api = inject(AccountActivationApi);
  private readonly router = inject(Router);
  private readonly bootstrap = inject(PrivateBootstrapStore);

  readonly state = signal<ActivationState>('ready');
  readonly submitError = signal<string | null>(null);

  readonly tenant = computed(() => this.bootstrap.tenantContext());
  readonly user = computed(() => this.bootstrap.user());

  readonly form = this.fb.group({
    currentTemporaryPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phoneNumber: [''],
  });

  async submit(): Promise<void> {
    this.submitError.set(null);

    if (this.form.invalid || this.form.value.newPassword !== this.form.value.confirmPassword) {
      this.form.markAllAsTouched();
      this.state.set('invalid');
      return;
    }

    this.state.set('submitting');

    try {
      await this.auth.changePassword(
        this.form.value.currentTemporaryPassword!,
        this.form.value.newPassword!,
      );
      const activation = await firstValueFrom(this.api.completeFirstLogin({
        firstName: this.form.value.firstName!,
        lastName: this.form.value.lastName!,
        phoneNumber: this.form.value.phoneNumber || null,
        passwordChanged: true,
      }));
      await this.auth.refreshSession(true);
      this.state.set('success');
      await this.router.navigateByUrl(activation.entryRoute ?? '/app/admin');
    } catch (err: unknown) {
      this.state.set('submitError');
      const problem = (err as { error?: { title?: string; detail?: string }; message?: string })?.error;
      this.submitError.set(problem?.detail ?? problem?.title ?? (err as { message?: string })?.message ?? null);
    }
  }
}
