import { AbstractControl, FormGroup } from '@angular/forms';
import { WebAppError } from '@tch/api';

import { resolveErrorFeedbackCopy, TranslateLookup } from './error-feedback-copy';

export interface ErrorViewModel {
  readonly title: string;
  readonly message: string;
  readonly severity: WebAppError['severity'];
  readonly code?: string;
}

export type FieldTargetMap = Readonly<Record<string, string>>;

export function errorsForTarget(
  errors: readonly WebAppError[],
  surface: WebAppError['surface'],
  target: string,
): readonly WebAppError[] {
  return errors.filter(error => error.surface === surface && error.target === target);
}

export function errorsForTargetPrefix(
  errors: readonly WebAppError[],
  surface: WebAppError['surface'],
  targetPrefix: string,
): readonly WebAppError[] {
  return errors.filter(
    error => error.surface === surface && error.target !== undefined && error.target.startsWith(targetPrefix),
  );
}

export function applyServerFieldErrors(
  form: FormGroup,
  errors: readonly WebAppError[],
  fieldTargets: FieldTargetMap = {},
): readonly WebAppError[] {
  const unconsumed: WebAppError[] = [];

  for (const error of errors) {
    if (error.surface !== 'field') {
      unconsumed.push(error);
      continue;
    }

    const control = controlForError(form, error, fieldTargets);
    if (!control) {
      unconsumed.push(error);
      continue;
    }

    control.setErrors({
      ...(control.errors ?? {}),
      server: error,
    });
    control.markAsTouched();
  }

  return unconsumed;
}

export function clearServerFieldErrors(control: AbstractControl): void {
  const errors = control.errors;
  if (errors?.['server']) {
    const rest = { ...errors };
    delete rest['server'];
    control.setErrors(Object.keys(rest).length ? rest : null);
  }

  if (control instanceof FormGroup) {
    Object.values(control.controls).forEach(child => clearServerFieldErrors(child));
  }
}

export function toErrorViewModel(error: WebAppError, copy: { title: string; message: string }): ErrorViewModel {
  return {
    title: copy.title,
    message: copy.message,
    severity: error.severity,
    code: error.code,
  };
}

export function withResolvedErrorCopy(error: WebAppError, translate: TranslateLookup): WebAppError {
  const copy = resolveErrorFeedbackCopy(error, translate);
  return {
    ...error,
    title: copy.title,
    message: copy.message,
  };
}

export function withResolvedErrorCopies(
  errors: readonly WebAppError[],
  translate: TranslateLookup,
): readonly WebAppError[] {
  return errors.map(error => withResolvedErrorCopy(error, translate));
}

function controlForError(
  form: FormGroup,
  error: WebAppError,
  fieldTargets: FieldTargetMap,
): AbstractControl | null {
  if (error.field) {
    const byField = form.get(error.field);
    if (byField) return byField;
  }

  if (error.target) {
    const field = fieldTargets[error.target];
    if (field) return form.get(field);

    const byTarget = form.get(error.target);
    if (byTarget) return byTarget;
  }

  return null;
}
