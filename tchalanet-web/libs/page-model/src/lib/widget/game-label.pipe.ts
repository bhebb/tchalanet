import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

/**
 * Translate a catalog game code (e.g. `HT_LOTO3`) to a human label.
 *
 * i18n key pattern: `catalog.game.<CODE>`
 *   fr → "Loto 3"
 *   en → "Lotto 3"
 *   ht → "Loto 3"
 *
 * Also handles bet-type codes via `catalog.bet_type.<CODE>`
 * and option labels via `catalog.option.<LABEL>`.
 *
 * Impure so it re-evaluates on language change.
 */
@Pipe({ name: 'gameLabel', pure: false })
export class GameLabelPipe implements PipeTransform {
  private readonly translate = inject(TranslateService);

  /**
   * @param code   A GameCode, BetType code, or option label string.
   * @param prefix i18n prefix — defaults to `catalog.game`.
   *               Pass `'catalog.bet_type'` or `'catalog.option'` for other catalog enums.
   */
  transform(code: string | null | undefined, prefix = 'catalog.game'): string {
    if (!code) return '';
    const key = `${prefix}.${code}`;
    const value = this.translate.instant(key);
    // ngx-translate returns the key itself when missing — fall back to a readable label
    return value && value !== key ? value : humanize(code);
  }
}

/** Convert an enum-style code to a readable fallback: HT_LOTO3 → "Loto 3" */
function humanize(code: string): string {
  return code
    .replace(/^HT_/, '')          // strip provider prefix
    .replace(/_/g, ' ')           // underscores → spaces
    .toLowerCase()
    .replace(/\b\w/g, c => c.toUpperCase()); // Title Case
}
