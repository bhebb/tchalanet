import { AbstractControl } from '@angular/forms';
import { ChangeDetectionStrategy, Component, computed, Input, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'tch-field-error',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  template: `
    @for (message of displayMessages(); track message) {
      <p class="tch-field-error" role="alert">{{ message | translate }}</p>
    }
  `,
  styles: [`
    :host {
      --comp-field-error-fg: var(--tch-color-error);
      --comp-field-error-font-size: var(--tch-font-size-label-sm);
    }
    .tch-field-error { margin: .25rem 0 0; color: var(--comp-field-error-fg); font-size: var(--comp-field-error-font-size); }
  `],
})
export class TchFieldError {
  private readonly messageValue = signal('');
  private readonly controlValue = signal<AbstractControl | null>(null);

  @Input() set message(value: string) { this.messageValue.set(value); }
  @Input() set control(value: AbstractControl | null) { this.controlValue.set(value); }

  readonly displayMessages = computed(() => {
    if (this.messageValue()) return [this.messageValue()];
    return controlErrorMessages(this.controlValue());
  });
}

function controlErrorMessages(control: AbstractControl | null): readonly string[] {
  if (!control || !control.touched || !control.errors) return [];

  const serverMessages = messagesForServerError(control.errors['server']);
  if (serverMessages.length) return serverMessages;

  if (control.errors['required']) return ['common.validation.required'];
  if (control.errors['email']) return ['common.validation.email'];
  if (
    control.errors['minlength'] ||
    control.errors['maxlength'] ||
    control.errors['min'] ||
    control.errors['max'] ||
    control.errors['pattern']
  ) {
    return ['common.validation.invalid'];
  }

  return [];
}

function messagesForServerError(value: unknown): readonly string[] {
  const values = Array.isArray(value) ? value : [value];
  return values.flatMap(value => isMessageCarrier(value) ? [value.message] : []);
}

function isMessageCarrier(value: unknown): value is { readonly message: string } {
  return typeof value === 'object' && value !== null &&
    'message' in value && typeof (value as { message?: unknown }).message === 'string';
}
