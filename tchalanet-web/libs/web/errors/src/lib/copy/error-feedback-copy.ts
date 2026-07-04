import { WebAppError } from '@tch/api';

export interface TchErrorFeedbackCopy {
  readonly title: string;
  readonly message: string;
}

export type TchTranslateLookup = (key: string) => string;

export type ErrorFeedbackCopy = TchErrorFeedbackCopy;
export type TranslateLookup = TchTranslateLookup;

export function resolveErrorFeedbackCopy(
  error: WebAppError,
  translate: TchTranslateLookup,
): TchErrorFeedbackCopy {
  if (error.code) {
    const codeTitle = translateIfPresent(translate, `common.errors.codes.${error.code}.title`);
    const codeMessage = translateIfPresent(translate, `common.errors.codes.${error.code}.message`);
    if (codeTitle && codeMessage) {
      return { title: codeTitle, message: codeMessage };
    }
    const fallback = fallbackForCode(error.code);
    if (fallback) {
      return fallback;
    }
  }

  const categoryTitle = translateIfPresent(
    translate,
    `common.errors.categories.${error.category}.title`,
  );
  const categoryMessage = translateIfPresent(
    translate,
    `common.errors.categories.${error.category}.message`,
  );
  if (categoryTitle && categoryMessage) {
    return { title: categoryTitle, message: categoryMessage };
  }

  return {
    title: translateIfPresent(translate, 'common.errors.fallback.title') ?? error.title,
    message: translateIfPresent(translate, 'common.errors.fallback.message') ?? error.message,
  };
}

function fallbackForCode(code: string): TchErrorFeedbackCopy | undefined {
  switch (code) {
    case 'entitlement.feature_required':
      return {
        title: 'Fonction non incluse dans le plan',
        message: 'Le plan actif de ce tenant ne permet pas cette fonctionnalite.',
      };
    case 'entitlement.limit_missing':
      return {
        title: 'Limite de plan manquante',
        message: 'Aucune limite active ne couvre cette action pour le tenant.',
      };
    case 'entitlement.limit_exceeded':
      return {
        title: 'Quota du plan atteint',
        message: 'Cette action depasse la limite autorisee par le plan actif.',
      };
    default:
      return undefined;
  }
}

function translateIfPresent(translate: TchTranslateLookup, key: string): string | undefined {
  const value = translate(key);
  return value && value !== key ? value : undefined;
}
