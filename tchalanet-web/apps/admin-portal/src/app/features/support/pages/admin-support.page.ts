import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '@tch/core/auth';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { TchNotice } from '@tch/ui/components';

import { AdminSupportApi } from '../data-access/admin-support-api.service';

type AdminSupportFormState = 'idle' | 'submitting' | 'success' | 'error';

@Component({
  selector: 'tch-admin-support-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchNotice,
  ],
  templateUrl: './admin-support.page.html',
  styleUrls: ['./admin-support.page.scss'],
})
export class AdminSupportPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AdminSupportApi);
  private readonly auth = inject(AuthSessionService);

  readonly state = signal<AdminSupportFormState>('idle');

  readonly form = this.fb.nonNullable.group({
    fullName: [''],
    phone: [''],
    email: ['', Validators.email],
    organizationName: [''],
    city: [''],
    country: [''],
    message: ['', [Validators.required, Validators.maxLength(2000)]],
    consentToContact: [false, Validators.requiredTrue],
  });

  submit(): void {
    if (this.form.invalid || this.state() === 'submitting') {
      this.form.markAllAsTouched();
      return;
    }

    this.state.set('submitting');
    const raw = this.form.getRawValue();
    const session = this.auth.session();
    const sessionName = session.displayName || session.username || 'Admin';
    const sessionEmail = emailValue(session.username);
    const contact = raw.phone || raw.email || sessionEmail || session.username || sessionName;
    this.api.submit({
      intent: 'SUPPORT',
      fullName: raw.fullName || sessionName,
      phone: contact,
      email: raw.email || sessionEmail,
      organizationName: raw.organizationName || undefined,
      city: raw.city || undefined,
      country: raw.country || undefined,
      message: raw.message,
      consentToContact: raw.consentToContact,
      sourcePage: window.location.pathname,
    }).subscribe({
      next: () => this.state.set('success'),
      error: () => this.state.set('error'),
    });
  }

  reset(): void {
    this.form.reset({ consentToContact: false });
    this.state.set('idle');
  }
}

function emailValue(value: string | undefined): string | undefined {
  return value?.includes('@') ? value : undefined;
}
