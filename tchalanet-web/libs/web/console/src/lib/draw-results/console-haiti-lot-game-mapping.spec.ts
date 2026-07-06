import { consoleHaitiLotGameMappings } from './console-haiti-lot-game-mapping';

describe('consoleHaitiLotGameMappings', () => {
  it('maps NY Haiti lots to Numbers and Win 4 provider products', () => {
    const mappings = consoleHaitiLotGameMappings({ slotKey: 'NY_MID' });

    expect(mappings.map(item => item.providerGameLabel)).toEqual(['Numbers', 'Win 4', 'Win 4']);
    expect(mappings[0].providerGameLogoUrl).toContain('/assets/images/lottery/ny_pick3.svg');
    expect(mappings[1].providerGameLogoUrl).toContain('/assets/images/lottery/ny_pick4.svg');
  });

  it('maps TX Pick 4 to the provider-specific Daily 4 logo', () => {
    const mappings = consoleHaitiLotGameMappings({ providerCode: 'TX' });

    expect(mappings.map(item => item.providerGameLabel)).toEqual(['Pick 3', 'Daily 4', 'Daily 4']);
    expect(mappings[1].providerGameLogoUrl).toContain('/assets/images/lottery/tx_pick4.png');
  });

  it('falls back to generic Pick 3 and Pick 4 products for unknown providers', () => {
    const mappings = consoleHaitiLotGameMappings({ slotKey: 'ZZ_NIGHT' });

    expect(mappings.map(item => item.providerGameLabel)).toEqual(['Pick 3', 'Pick 4', 'Pick 4']);
    expect(mappings[0].providerGameLogoUrl).toContain('/assets/images/lottery/pick3-logo.png');
    expect(mappings[1].providerGameLogoUrl).toContain('/assets/images/lottery/pick4-logo.png');
  });
});
