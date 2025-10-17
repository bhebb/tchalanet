import { Inject, Injectable } from '@angular/core';
import { InstantSearchClient } from '../../../../../shared/api/src/lib/client/instant-search-client';
import { SearchResponse } from 'meilisearch';

export interface SearchResult<T = any> {
  id: string; // Identifiant unique du résultat
  type: string; // Type de résultat (ex: 'lottery', 'product', 'draw')
  title: string; // Titre principal du résultat
  description?: string; // Description courte
  metadata?: T; // Données spécifiques au type de résultat
  score?: number; // Score de pertinence de la recherche
  highlights?: {
    [key: string]: string; // Passages mis en évidence
  };
  links?: {
    self?: string; // Lien vers le détail
    preview?: string; // Lien vers un aperçu
  };
}

export interface DrawResult {
  lotteryId?: string;
  lotteryName?: string;
  drawTime?: string; // ISO
  numbers?: number[];
  bonus?: number[];
}

// Exemple spécifique pour les tirages
export interface DrawSearchResult extends SearchResult<DrawResult> {
  type: 'draw';
  metadata: DrawResult;
}

@Injectable({
  providedIn: 'root',
})
export class SearchService {
  constructor(
    @Inject(InstantSearchClient)
    private searchClient: InstantSearchClient,
  ) {}

  async searchDraws(query: string): Promise<DrawSearchResult[]> {
    try {
      const results = await this.searchClient.search<DrawSearchResult>('draws', query, {
        attributesToRetrieve: ['id', 'lotteryName', 'drawTime', 'numbers', 'bonus'],
        limit: 10,
      });

      // Transformation des résultats Meilisearch en SearchResult
      return results.hits.map(
        hit =>
          ({
            id: hit.id,
            type: 'draw',
            title: hit.title,
            description: `Tirage du ${new Date().toLocaleDateString()}`,
            metadata: hit,
            links: {
              self: `/draws/${hit.id}`,
              preview: `/draws/${hit.id}/preview`,
            },
          } as DrawSearchResult),
      );
    } catch (error) {
      console.error('Search draws failed', error);
      return [];
    }
  }

  async multiIndexSearch(
    query: string,
    options?: {
      indexes?: string[];
      limit?: number;
    }
  ): Promise<Record<string, SearchResponse<SearchResult>>> {
    const indexes = options?.indexes ?? ['draws', 'lotteries', 'products'];
    const limit = options?.limit ?? 5;

    const searches = indexes.map(async indexName => {
      try {
        // Vérification préalable des documents
        await this.checkIndexDocuments(indexName);

        // Ajout de documents de test si nécessaire
        await this.addTestDocuments(indexName);

        const result = await this.searchClient.search(indexName, query, {
          limit: limit,
          attributesToRetrieve: ['id', 'name', 'title', 'description', 'lotteryName'],
        });

        console.log(`Search results for ${indexName}:`, result.hits);

        return { [indexName]: result };
      } catch (error) {
        console.error(`Search failed for index ${indexName}`, error);

        // Tentative d'ajout de documents si échec
        try {
          await this.addTestDocuments(indexName);
        } catch (addError) {
          console.error(`Failed to add test documents to ${indexName}`, addError);
        }

        return {
          [indexName]: {
            hits: [],
            query,
            processingTimeMs: 0,
            limit: 0
          }
        };
      }
    });

    const results = await Promise.all(searches);
    return Object.assign({}, ...results);
  }

  async checkIndexDocuments(indexName: string) {
    try {
      const client = await this.searchClient.ensureClient();
      const index = client.index(indexName);

      // Récupérer tous les documents
      const documents = await index.getDocuments({
        limit: 100, // Ajustez selon vos besoins
        offset: 0,
      });

      console.log(`Documents in index ${indexName}:`, documents.results);
      console.log(`Total documents: ${documents.total}`);

      return documents.results;
    } catch (error) {
      console.error(`Error fetching documents from ${indexName}:`, error);
      return [];
    }
  }

  // Ajoutez une méthode d'ajout de documents
  async addTestDocuments(indexName: string) {
    const testDocuments = {
      draws: [
        {
          id: '1',
          lotteryName: 'Euromillions',
          drawTime: new Date().toISOString(),
          numbers: [1, 2, 3, 4, 5],
          bonus: [6],
          title: 'Euromillions Draw',
        },
      ],
      lotteries: [
        {
          id: '1',
          name: 'Euromillions Lottery',
          description: 'Main European lottery',
          title: 'Euromillions',
        },
      ],
      products: [
        {
          id: '1',
          name: 'Test Product',
          description: 'A test product for search',
          title: 'Test Product',
        },
      ],
    };

    try {
      const client = await this.searchClient.ensureClient();
      const index = client.index(indexName);

      // @ts-ignore
      const documents = testDocuments[indexName] || [];

      if (documents.length > 0) {
        const result = await index.addDocuments(documents);
        console.log(`Added documents to ${indexName}:`, result);
        return result;
      }
    } catch (error) {
      console.error(`Error adding documents to ${indexName}:`, error);
    }
    return []
  }
}
