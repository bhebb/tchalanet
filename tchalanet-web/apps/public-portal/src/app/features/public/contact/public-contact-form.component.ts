import { ChangeDetectionStrategy, Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { CONTACT_INTENTS, ContactIntent } from './public-contact.model';
import { PublicContactService } from './public-contact.service';

type FormState = 'idle' | 'submitting' | 'success' | 'error';

@Component({
  selector: 'tch-public-contact-form',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-contact-form.component.html',
  styleUrls: ['./public-contact-form.component.scss'],
})
export class PublicContactFormComponent implements OnInit {
  @Input() intent: ContactIntent = 'REQUEST_DEMO';
  @Input() lockIntent = false;
  @Input() prefillMessage = '';

  private readonly fb = inject(FormBuilder);
  private readonly contactService = inject(PublicContactService);

  readonly state = signal<FormState>('idle');
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

  ngOnInit(): void {
    this.form.patchValue({ intent: this.intent, message: this.prefillMessage });
    if (this.lockIntent) {
      this.form.controls.intent.disable();
    }
  }

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
    this.form.reset({ intent: this.intent, consentToContact: false });
    if (this.lockIntent) {
      this.form.controls.intent.disable();
    }
    this.state.set('idle');
  }
}
