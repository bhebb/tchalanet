import { ApiNotice, ProblemDetail, ServiceStatus, TchDiagnosticInfo } from '../contracts/api.types';

export type WebErrorOrigin = 'backend' | 'frontend' | 'network' | 'validation' | 'auth';
export type WebErrorCategory =
  | 'auth_required'
  | 'access_denied'
  | 'validation'
  | 'not_found'
  | 'conflict'
  | 'rate_limited'
  | 'network_unavailable'
  | 'service_unavailable'
  | 'unexpected';
export type WebErrorSeverity = 'info' | 'warn' | 'error';
export type WebErrorSurface = 'page' | 'section' | 'field' | 'shell';
export type WebErrorPlacement = 'top' | 'inline' | 'summary';

export interface WebAppError {
  readonly id: string;
  readonly origin: WebErrorOrigin;
  readonly category: WebErrorCategory;
  readonly severity: WebErrorSeverity;
  readonly surface: WebErrorSurface;
  readonly placement: WebErrorPlacement;
  readonly title: string;
  readonly message: string;
  readonly code?: string;
  readonly status?: number;
  readonly requestId?: string;
  readonly traceId?: string;
  readonly spanId?: string;
  readonly errorId?: string;
  readonly source?: string;
  readonly target?: string;
  readonly field?: string;
  readonly retryable: boolean;
  readonly dedupeKey: string;
}

export function webAppErrorFromProblemDetail(
  problem: ProblemDetail,
  source: string,
  surface: WebErrorSurface = 'shell',
): WebAppError {
  const code = problem.code ?? problemTypeCode(problem.type);
  const category = categoryFromCodeStatus(code, problem.status);
  const severity = problem.status >= 500 || problem.status === 0 ? 'error' : 'warn';
  const resolvedSource = problem.instance ?? source;

  return {
    id: stableErrorId(
      'problem',
      code,
      category,
      resolvedSource,
      problem.requestId,
      problem.traceId,
      problem.errorId,
    ),
    origin: problem.status === 401 ? 'auth' : 'backend',
    category,
    severity,
    surface,
    placement: surface === 'field' ? 'inline' : 'top',
    title: userSafeTitle(code, category, problem.title),
    message: userSafeMessage(code, category, problem.detail ?? problem.title),
    code,
    status: problem.status,
    requestId: problem.requestId ?? problem.correlationId,
    traceId: problem.traceId,
    spanId: problem.spanId,
    errorId: problem.errorId,
    source: resolvedSource,
    target: undefined,
    field: undefined,
    retryable: isRetryable(category, problem.status),
    dedupeKey: dedupeKey(code, category, problem.status, resolvedSource, surface),
  };
}

export function webAppErrorFromNotice(
  notice: ApiNotice,
  trace: TchDiagnosticInfo | undefined,
  source: string,
  surface: WebErrorSurface = 'shell',
): WebAppError {
  const meta = notice.meta ?? {};
  const noticeSource = stringMeta(meta, 'source') ?? notice.target ?? notice.domain ?? source;
  const status = numberMeta(meta, 'status');
  const category = categoryFromCodeStatus(notice.code, status);
  const severity = severityFromNotice(notice.severity);
  const requestId = stringMeta(meta, 'requestId') ?? trace?.requestId;
  const traceId = stringMeta(meta, 'traceId') ?? trace?.traceId;
  const spanId = stringMeta(meta, 'spanId') ?? trace?.spanId;
  const errorId = stringMeta(meta, 'errorId');
  const resolvedSurface = surfaceFromMeta(meta) ?? surface;
  const placement = placementFromMeta(meta) ?? defaultPlacement(resolvedSurface);
  const target = stringMeta(meta, 'target') ?? notice.target;
  const field = stringMeta(meta, 'field');

  return {
    id: stableErrorId('notice', notice.code, category, noticeSource, requestId, traceId, errorId),
    origin: 'backend',
    category,
    severity,
    surface: resolvedSurface,
    placement,
    title: userSafeTitle(notice.code, category, notice.message),
    message: userSafeMessage(notice.code, category, notice.message),
    code: notice.code,
    status,
    requestId,
    traceId,
    spanId,
    errorId,
    source: noticeSource,
    target,
    field,
    retryable: isRetryable(category, status),
    dedupeKey: dedupeKey(
      notice.code,
      category,
      status,
      noticeSource,
      resolvedSurface,
      target,
      field,
    ),
  };
}

export function webAppErrorsFromProblemDetailFields(
  problem: ProblemDetail,
  source: string,
): readonly WebAppError[] {
  const violations = problem.violations?.length
    ? problem.violations
    : Object.entries(problem.errors ?? {}).flatMap(([field, messages]) =>
        messages.map(message => ({ code: undefined, field, message, target: field })),
      );

  return violations.map(violation => {
    const code = violation.code ?? problem.code ?? 'validation.failed';
    const target = violation.target ?? violation.field;
    const message = violation.message ?? problem.detail ?? problem.title;
    const category: WebErrorCategory = 'validation';

    return {
      id: stableErrorId(
        'field',
        code,
        category,
        target,
        problem.requestId,
        problem.traceId,
        problem.errorId,
      ),
      origin: 'validation',
      category,
      severity: 'error',
      surface: 'field',
      placement: 'inline',
      title: userSafeTitle(code, category, problem.title),
      message: userSafeMessage(code, category, message),
      code,
      status: problem.status,
      requestId: problem.requestId ?? problem.correlationId,
      traceId: problem.traceId,
      spanId: problem.spanId,
      errorId: problem.errorId,
      source,
      target,
      field: violation.field,
      retryable: false,
      dedupeKey: dedupeKey(
        code,
        category,
        problem.status,
        source,
        'field',
        target,
        violation.field,
      ),
    } satisfies WebAppError;
  });
}

export function webAppErrorFromServiceStatus(
  service: ServiceStatus,
  trace: TchDiagnosticInfo | undefined,
  source: string,
): WebAppError {
  const code = `service.${service.service}.${service.status.toLowerCase()}`;
  const category: WebErrorCategory =
    service.status === 'DOWN' ? 'service_unavailable' : 'unexpected';
  const severity: WebErrorSeverity = service.status === 'DOWN' ? 'error' : 'warn';
  const resolvedSource = service.service || source;

  return {
    id: stableErrorId(
      'service',
      code,
      category,
      resolvedSource,
      trace?.requestId,
      trace?.traceId,
      undefined,
    ),
    origin: 'backend',
    category,
    severity,
    surface: 'shell',
    placement: 'top',
    title: userSafeTitle(code, category, service.message ?? resolvedSource),
    message: userSafeMessage(code, category, service.message ?? resolvedSource),
    code,
    requestId: trace?.requestId,
    traceId: trace?.traceId,
    spanId: trace?.spanId,
    source: resolvedSource,
    target: undefined,
    field: undefined,
    retryable: true,
    dedupeKey: dedupeKey(code, category, undefined, resolvedSource, 'shell', undefined, undefined),
  };
}

function categoryFromCodeStatus(
  code: string | undefined,
  status: number | undefined,
): WebErrorCategory {
  const normalized = code?.toLowerCase() ?? '';
  if (normalized.includes('access.denied') || normalized.includes('forbidden'))
    return 'access_denied';
  if (normalized.includes('validation') || normalized.includes('request.')) return 'validation';
  if (normalized.includes('not_found') || normalized.includes('not-found')) return 'not_found';
  if (normalized.includes('conflict')) return 'conflict';
  if (normalized.includes('rate')) return 'rate_limited';
  if (
    normalized.includes('service') ||
    normalized.includes('unavailable') ||
    normalized.includes('degraded')
  ) {
    return 'service_unavailable';
  }

  switch (status) {
    case 0:
      return 'network_unavailable';
    case 400:
    case 422:
      return 'validation';
    case 401:
      return 'auth_required';
    case 403:
      return 'access_denied';
    case 404:
      return 'not_found';
    case 409:
      return 'conflict';
    case 429:
      return 'rate_limited';
    default:
      if (status !== undefined && status >= 500) return 'service_unavailable';
      return 'unexpected';
  }
}

function severityFromNotice(severity: ApiNotice['severity']): WebErrorSeverity {
  switch (severity) {
    case 'ERROR':
    case 'error':
      return 'error';
    case 'WARN':
    case 'warning':
      return 'warn';
    default:
      return 'info';
  }
}

function isRetryable(category: WebErrorCategory, status: number | undefined): boolean {
  return category === 'network_unavailable' || category === 'service_unavailable' || status === 429;
}

function dedupeKey(
  code: string | undefined,
  category: WebErrorCategory,
  status: number | undefined,
  source: string | undefined,
  surface: WebErrorSurface,
  target: string | undefined = undefined,
  field: string | undefined = undefined,
): string {
  return [
    code || category,
    status ?? 'na',
    source || 'unknown',
    surface,
    target || 'na',
    field || 'na',
  ].join('|');
}

function stableErrorId(
  kind: string,
  code: string | undefined,
  category: WebErrorCategory,
  source: string | undefined,
  requestId: string | undefined,
  traceId: string | undefined,
  errorId: string | undefined,
): string {
  return [kind, errorId, requestId, traceId, code, category, source].filter(Boolean).join('|');
}

function userSafeTitle(
  code: string | undefined,
  category: WebErrorCategory,
  fallback: string,
): string {
  if (code) return code;
  switch (category) {
    case 'auth_required':
      return 'Session requise';
    case 'access_denied':
      return 'Acces non autorise';
    case 'validation':
      return 'Information a corriger';
    case 'not_found':
      return 'Element introuvable';
    case 'conflict':
      return 'Action impossible';
    case 'rate_limited':
      return 'Trop de demandes';
    case 'network_unavailable':
      return 'Connexion indisponible';
    case 'service_unavailable':
      return 'Service temporairement indisponible';
    default:
      return fallback || 'Erreur inattendue';
  }
}

function userSafeMessage(
  code: string | undefined,
  category: WebErrorCategory,
  fallback: string,
): string {
  if (code) return fallback || code;
  switch (category) {
    case 'auth_required':
      return 'Reconnectez-vous pour continuer.';
    case 'access_denied':
      return 'Votre compte ne permet pas cette action.';
    case 'validation':
      return 'Verifiez les champs et reessayez.';
    case 'not_found':
      return 'La ressource demandee n est plus disponible.';
    case 'conflict':
      return 'L etat actuel ne permet pas de terminer cette action.';
    case 'rate_limited':
      return 'Patientez un moment avant de reessayer.';
    case 'network_unavailable':
      return 'Verifiez la connexion et reessayez.';
    case 'service_unavailable':
      return 'Une partie du service est temporairement indisponible.';
    default:
      return 'Un probleme est survenu. Copiez la reference support si le probleme persiste.';
  }
}

function problemTypeCode(type: string | undefined): string | undefined {
  if (!type || type === 'about:blank') return undefined;
  const parts = type.split('/').filter(Boolean);
  return parts[parts.length - 1];
}

function stringMeta(meta: Readonly<Record<string, unknown>>, key: string): string | undefined {
  const value = meta[key];
  return typeof value === 'string' && value.trim() ? value : undefined;
}

function surfaceFromMeta(meta: Readonly<Record<string, unknown>>): WebErrorSurface | undefined {
  const value = stringMeta(meta, 'surface');
  return value === 'shell' || value === 'page' || value === 'section' || value === 'field'
    ? value
    : undefined;
}

function placementFromMeta(meta: Readonly<Record<string, unknown>>): WebErrorPlacement | undefined {
  const value = stringMeta(meta, 'placement');
  return value === 'top' || value === 'inline' || value === 'summary' ? value : undefined;
}

function defaultPlacement(surface: WebErrorSurface): WebErrorPlacement {
  return surface === 'field' ? 'inline' : 'top';
}

function numberMeta(meta: Readonly<Record<string, unknown>>, key: string): number | undefined {
  const value = meta[key];
  return typeof value === 'number' ? value : undefined;
}
