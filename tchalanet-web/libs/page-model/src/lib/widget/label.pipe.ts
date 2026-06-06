import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { keyFallback } from './widget.contract';

/**
 * Translate an i18n key directly, but never hide content: when the translation is missing
 * (ngx-translate returns the key unchanged), render a stable fallback derived from the key.
 * Impure so it re-evaluates on language change.
 */
@Pipe({ name: 'tchLabel', pure: false })
export class LabelPipe implements PipeTransform {
  private readonly translate = inject(TranslateService);

  transform(key: string | undefined | null): string {
    if (!key) {
      return '';
    }
    const value = this.translate.instant(key);
    return value && value !== key ? value : keyFallback(key);
  }
}
