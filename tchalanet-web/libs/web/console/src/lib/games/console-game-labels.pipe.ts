import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import {
  consoleBetLabel,
  consoleBetOptionLabelKey,
  consoleBetOptionLabel,
  consoleBetTypeLabel,
  consoleBetTypeLabelKey,
  consoleGameLogoUrl,
  consoleGameLogoText,
  consoleGameName,
} from './console-game-display';

function translatedOrFallback(translate: TranslateService, key: string | null, fallback: string | null): string | null {
  if (!key) return fallback;
  const translated = translate.instant(key);
  return translated && translated !== key ? translated : fallback;
}

@Pipe({
  name: 'consoleGameName',
  standalone: true,
})
export class ConsoleGameNamePipe implements PipeTransform {
  transform(gameCode: string, displayName?: string | null): string {
    return consoleGameName(gameCode, displayName);
  }
}

@Pipe({
  name: 'consoleGameLogoText',
  standalone: true,
})
export class ConsoleGameLogoTextPipe implements PipeTransform {
  transform(gameCode: string, displayName?: string | null): string {
    return consoleGameLogoText(gameCode, displayName);
  }
}

@Pipe({
  name: 'consoleGameLogoUrl',
  standalone: true,
})
export class ConsoleGameLogoUrlPipe implements PipeTransform {
  transform(gameCode: string): string | null {
    return consoleGameLogoUrl(gameCode);
  }
}

@Pipe({
  name: 'consoleBetTypeLabel',
  standalone: true,
  pure: false,
})
export class ConsoleBetTypeLabelPipe implements PipeTransform {
  private readonly translate = inject(TranslateService);

  transform(betType: string): string {
    const fallback = consoleBetTypeLabel(betType);
    return translatedOrFallback(this.translate, consoleBetTypeLabelKey(betType), fallback) ?? fallback;
  }
}

@Pipe({
  name: 'consoleBetOptionLabel',
  standalone: true,
  pure: false,
})
export class ConsoleBetOptionLabelPipe implements PipeTransform {
  private readonly translate = inject(TranslateService);

  transform(betType: string, betOption: number | string | null = null): string | null {
    const fallback = consoleBetOptionLabel(betType, betOption);
    return translatedOrFallback(this.translate, consoleBetOptionLabelKey(betType, betOption), fallback);
  }
}

@Pipe({
  name: 'consoleBetLabel',
  standalone: true,
  pure: false,
})
export class ConsoleBetLabelPipe implements PipeTransform {
  private readonly translate = inject(TranslateService);

  transform(betType: string, betOption: number | string | null = null): string {
    const optionFallback = consoleBetOptionLabel(betType, betOption);
    const optionLabel = translatedOrFallback(
      this.translate,
      consoleBetOptionLabelKey(betType, betOption),
      optionFallback,
    );
    if (optionLabel) return optionLabel;

    const fallback = consoleBetLabel(betType, betOption);
    return translatedOrFallback(this.translate, consoleBetTypeLabelKey(betType), fallback) ?? fallback;
  }
}
