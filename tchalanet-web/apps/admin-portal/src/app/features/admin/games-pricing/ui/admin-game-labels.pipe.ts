import { Pipe, PipeTransform } from '@angular/core';

import { adminBetLabel, adminGameLogoText, adminGameName } from '../data-access/admin-game-display';

@Pipe({
  name: 'adminGameName',
  standalone: true,
})
export class AdminGameNamePipe implements PipeTransform {
  transform(gameCode: string, displayName?: string | null): string {
    return adminGameName(gameCode, displayName);
  }
}

@Pipe({
  name: 'adminGameLogoText',
  standalone: true,
})
export class AdminGameLogoTextPipe implements PipeTransform {
  transform(gameCode: string, displayName?: string | null): string {
    return adminGameLogoText(gameCode, displayName);
  }
}

@Pipe({
  name: 'adminBetLabel',
  standalone: true,
})
export class AdminBetLabelPipe implements PipeTransform {
  transform(betType: string, betOption: number | null = null): string {
    return adminBetLabel(betType, betOption);
  }
}
