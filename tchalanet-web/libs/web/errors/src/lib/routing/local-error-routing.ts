import { AbstractControl, FormGroup } from '@angular/forms';
import { WebAppError } from '@tch/api';

import { resolveErrorFeedbackCopy, TchTranslateLookup } from '../copy/error-feedback-copy';
import { TchErrorViewModel } from '../models/error-view-model';

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
    error =>
      error.surface === surface &&
      error.target !== undefined &&
      error.target.startsWith(targetPrefix),
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

export function toErrorViewModel(
  error: WebAppError,
  copy: { title: string; message: string },
): TchErrorViewModel {
  return {
    title: copy.title,
    message: copy.message,
    severity: error.severity,
    surface: error.surface,
    placement: error.placement,
    code: error.code,
    status: error.status,
    requestId: error.requestId,
    traceId: error.traceId,
    spanId: error.spanId,
    errorId: error.errorId,
    source: error.source,
    target: error.target,
    field: error.field,
    retryable: error.retryable,
    dedupeKey: error.dedupeKey,
  };
}

export function withResolvedErrorCopy(
  error: WebAppError,
  translate: TchTranslateLookup,
): WebAppError {
  const copy = resolveErrorFeedbackCopy(error, translate);
  return {
    ...error,
    title: copy.title,
    message: copy.message,
  };
}

export function withResolvedErrorCopies(
  errors: readonly WebAppError[],
  translate: TchTranslateLookup,
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
