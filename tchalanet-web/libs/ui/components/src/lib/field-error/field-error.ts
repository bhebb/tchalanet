import { AbstractControl } from '@angular/forms';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  inject,
  Input,
  OnDestroy,
  signal,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

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
export class TchFieldError implements OnDestroy {
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly messageValue = signal('');
  private readonly controlValue = signal<AbstractControl | null>(null);
  private readonly controlStatusVersion = signal(0);
  private controlStatusSubscription = new Subscription();

  @Input() set message(value: string) {
    this.messageValue.set(value);
  }
  @Input() set control(value: AbstractControl | null) {
    this.controlStatusSubscription.unsubscribe();
    this.controlStatusSubscription = value
      ? value.statusChanges.subscribe(() => {
          this.controlStatusVersion.update(version => version + 1);
          this.changeDetector.markForCheck();
        })
      : new Subscription();
    this.controlValue.set(value);
  }

  ngOnDestroy(): void {
    this.controlStatusSubscription.unsubscribe();
  }

  readonly displayMessages = computed(() => {
    this.controlStatusVersion();
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
