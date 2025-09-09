import { Inject, Injectable } from '@angular/core';
import { InstantSearchClient } from '../../../../../shared/api/src/lib/client/instant-search-client';

@Injectable({
  providedIn: 'root',
})
export class SearchIndexInitializerService {
  constructor(
    @Inject(InstantSearchClient)
    private searchClient: InstantSearchClient,
  ) {}

  async initializeIndexes() {
    const requiredIndexes = ['draws', 'lotteries', 'products'];

    for (const indexName of requiredIndexes) {
      try {
        // Vérifier si l'index existe, sinon le créer
        const indexExists = await this.checkOrCreateIndex(indexName);
        console.log(`Index ${indexName}: ${indexExists ? 'Exists' : 'Created'}`);
      } catch (error) {
        console.error(`Failed to initialize index ${indexName}:`, error);
      }
    }
  }

  private async checkOrCreateIndex(indexName: string): Promise<boolean> {
    try {
      // Tenter de récupérer les informations de l'index
      await this.searchClient.getIndex(indexName);
      return true;
    } catch (error) {
      // Si l'index n'existe pas, le créer
      try {
        await this.searchClient.createIndex(indexName);

        // Configuration de l'index (exemple)
        await this.configureIndex(indexName);

        return false;
      } catch (createError) {
        console.error(`Could not create index ${indexName}:`, createError);
        throw createError;
      }
    }
  }

  private async configureIndex(indexName: string) {
    // Configuration spécifique de l'index
    switch (indexName) {
      case 'draws':
        await this.searchClient.updateIndexSettings(indexName, {
          searchableAttributes: ['lotteryName', 'drawTime'],
          displayedAttributes: ['id', 'lotteryName', 'drawTime', 'numbers', 'bonus'],
        });
        break;
      case 'lotteries':
        await this.searchClient.updateIndexSettings(indexName, {
          searchableAttributes: ['name', 'description'],
          displayedAttributes: ['id', 'name', 'description'],
        });
        break;
      case 'products':
        await this.searchClient.updateIndexSettings(indexName, {
          searchableAttributes: ['name', 'description'],
          displayedAttributes: ['id', 'name', 'description', 'price'],
        });
        break;
    }
  }
}
