import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { CONTACT_INTENTS, ContactIntent } from './public-contact.model';
import { PublicContactService } from './public-contact.service';

type PageState = 'idle' | 'submitting' | 'success' | 'error';

@Component({
  selector: 'tch-public-contact-page',
  imports: [ReactiveFormsModule, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-contact.page.html',
  styleUrls: ['./public-contact.page.scss'],
})
export class PublicContactPage {
  private readonly fb = inject(FormBuilder);
  private readonly contactService = inject(PublicContactService);

  readonly state = signal<PageState>('idle');
  readonly intents: readonly ContactIntent[] = CONTACT_INTENTS;

  readonly form = this.fb.nonNullable.group({
    intent: ['REQUEST_DEMO' as ContactIntent, Validators.required],
    fullName: ['', Validators.required],
    phone: ['', Validators.required],
    email: ['', Validators.email],
    organizationName: [''],
    city: [''],
    country: [''],
    message: ['', Validators.required],
    consentToContact: [false, Validators.requiredTrue],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.state.set('submitting');

    const raw = this.form.getRawValue();
    this.contactService
      .submit({
        intent: raw.intent,
        fullName: raw.fullName,
        phone: raw.phone,
        email: raw.email || undefined,
        organizationName: raw.organizationName || undefined,
        city: raw.city || undefined,
        country: raw.country || undefined,
        message: raw.message,
        consentToContact: raw.consentToContact,
        sourcePage: window.location.pathname,
      })
      .subscribe({
        next: () => this.state.set('success'),
        error: () => this.state.set('error'),
      });
  }

  reset(): void {
    this.form.reset({ intent: 'REQUEST_DEMO', consentToContact: false });
    this.state.set('idle');
  }
}
