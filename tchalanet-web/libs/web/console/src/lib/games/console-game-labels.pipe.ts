import { Pipe, PipeTransform } from '@angular/core';

import {
  consoleBetLabel,
  consoleBetOptionLabel,
  consoleBetTypeLabel,
  consoleGameLogoUrl,
  consoleGameLogoText,
  consoleGameName,
} from './console-game-display';

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
})
export class ConsoleBetTypeLabelPipe implements PipeTransform {
  transform(betType: string): string {
    return consoleBetTypeLabel(betType);
  }
}

@Pipe({
  name: 'consoleBetOptionLabel',
  standalone: true,
})
export class ConsoleBetOptionLabelPipe implements PipeTransform {
  transform(betType: string, betOption: number | string | null = null): string | null {
    return consoleBetOptionLabel(betType, betOption);
  }
}

@Pipe({
  name: 'consoleBetLabel',
  standalone: true,
})
export class ConsoleBetLabelPipe implements PipeTransform {
  transform(betType: string, betOption: number | string | null = null): string {
    return consoleBetLabel(betType, betOption);
  }
}
