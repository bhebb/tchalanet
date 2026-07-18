import { WebAppError } from '@tch/api';

export interface TchErrorFeedbackCopy {
  readonly title: string;
  readonly message: string;
}

export type TchTranslateLookup = (key: string) => string;

export type ErrorFeedbackCopy = TchErrorFeedbackCopy;
export type TranslateLookup = TchTranslateLookup;

const SAFE_GENERIC_COPY: TchErrorFeedbackCopy = {
  title: 'Unexpected error',
  message: 'Try again or contact support if the problem persists.',
};

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
    title: translateIfPresent(translate, 'common.errors.fallback.title') ?? SAFE_GENERIC_COPY.title,
    message: translateIfPresent(translate, 'common.errors.fallback.message') ?? SAFE_GENERIC_COPY.message,
  };
}

function translateIfPresent(translate: TchTranslateLookup, key: string): string | undefined {
  const value = translate(key);
  return value && value !== key ? value : undefined;
}
