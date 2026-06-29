import {
  ProblemDetail,
  WebAppError,
  WebErrorSurface,
  mapHttpErrorToProblemDetail,
  webAppErrorFromProblemDetail,
  webAppErrorsFromProblemDetailFields,
} from '@tch/api';

import { resolveErrorFeedbackCopy, TchTranslateLookup } from '../copy/error-feedback-copy';
import { TchErrorViewModel } from '../models/error-view-model';
import { toErrorViewModel } from '../routing/local-error-routing';

export interface TchApiErrorPresenterOptions {
  readonly source: string;
  readonly surface?: WebErrorSurface;
}

export interface TchPresentedApiError {
  readonly error: WebAppError;
  readonly viewModel: TchErrorViewModel;
}

export function presentWebAppError(
  error: WebAppError,
  translate: TchTranslateLookup,
): TchPresentedApiError {
  const copy = resolveErrorFeedbackCopy(error, translate);
  return {
    error,
    viewModel: toErrorViewModel(error, copy),
  };
}

export function presentApiError(
  err: unknown,
  translate: TchTranslateLookup,
  options: TchApiErrorPresenterOptions,
): TchPresentedApiError {
  const error = normalizeApiError(err, options);
  return presentWebAppError(error, translate);
}

export function normalizeApiError(
  err: unknown,
  options: TchApiErrorPresenterOptions,
): WebAppError {
  if (isWebAppError(err)) {
    return err;
  }

  const problem = isProblemDetailLike(err) ? err : mapHttpErrorToProblemDetail(err);
  return webAppErrorFromProblemDetail(problem, options.source, options.surface ?? 'page');
}

export function fieldErrorsFromApiError(err: unknown, source: string): readonly WebAppError[] {
  const problem = isProblemDetailLike(err) ? err : mapHttpErrorToProblemDetail(err);
  return webAppErrorsFromProblemDetailFields(problem, source);
}

export function presentFieldErrorsFromApiError(
  err: unknown,
  translate: TchTranslateLookup,
  source: string,
): readonly TchPresentedApiError[] {
  return fieldErrorsFromApiError(err, source).map(error => presentWebAppError(error, translate));
}

function isWebAppError(value: unknown): value is WebAppError {
  return (
    isRecord(value) &&
    typeof value['id'] === 'string' &&
    typeof value['category'] === 'string' &&
    typeof value['surface'] === 'string' &&
    typeof value['dedupeKey'] === 'string'
  );
}

function isProblemDetailLike(value: unknown): value is ProblemDetail {
  return isRecord(value) && typeof value['title'] === 'string' && typeof value['status'] === 'number';
}

function isRecord(value: unknown): value is Readonly<Record<string, unknown>> {
  return typeof value === 'object' && value !== null;
}
