import { Pipe, PipeTransform } from '@angular/core';
import { ChannelGameSetupView } from '../data-access/admin-draw-sales-matrix-api.service';

@Pipe({ name: 'offeredGames', standalone: true, pure: true })
export class OfferedGamesPipe implements PipeTransform {
  transform(games: ChannelGameSetupView[]): ChannelGameSetupView[] {
    return games.filter(g => g.offeredOnChannel);
  }
}

@Pipe({ name: 'availableGames', standalone: true, pure: true })
export class AvailableGamesPipe implements PipeTransform {
  transform(games: ChannelGameSetupView[]): ChannelGameSetupView[] {
    return games.filter(g => !g.offeredOnChannel && g.enabledForTenant);
  }
}
