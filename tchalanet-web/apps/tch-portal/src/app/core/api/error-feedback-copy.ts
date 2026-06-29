import { WebAppError } from '@tch/api';

export interface ErrorFeedbackCopy {
  readonly title: string;
  readonly message: string;
}

export type TranslateLookup = (key: string) => string;

export function resolveErrorFeedbackCopy(error: WebAppError, translate: TranslateLookup): ErrorFeedbackCopy {
  if (error.code) {
    const codeTitle = translateIfPresent(translate, `common.errors.codes.${error.code}.title`);
    const codeMessage = translateIfPresent(translate, `common.errors.codes.${error.code}.message`);
    if (codeTitle && codeMessage) {
      return { title: codeTitle, message: codeMessage };
    }
  }

  const categoryTitle = translateIfPresent(translate, `common.errors.categories.${error.category}.title`);
  const categoryMessage = translateIfPresent(translate, `common.errors.categories.${error.category}.message`);
  if (categoryTitle && categoryMessage) {
    return { title: categoryTitle, message: categoryMessage };
  }

  return {
    title: translateIfPresent(translate, 'common.errors.fallback.title') ?? error.title,
    message: translateIfPresent(translate, 'common.errors.fallback.message') ?? error.message,
  };
}

function translateIfPresent(translate: TranslateLookup, key: string): string | undefined {
  const value = translate(key);
  return value && value !== key ? value : undefined;
}
