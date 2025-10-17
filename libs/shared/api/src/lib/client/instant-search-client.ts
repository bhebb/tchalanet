import { MeiliSearch, RecordAny, SearchParams, SearchResponse } from 'meilisearch';
import { environment } from '@tchl/config';
import { InjectionToken } from '@angular/core';

// Créer un token d'injection
export const MEILISEARCH_CONFIG = new InjectionToken<MeilisearchConfig>('MEILISEARCH_CONFIG');

// Interface pour la configuration
export interface MeilisearchConfig {
  host: string;
  apiKey?: string;
}

export class InstantSearchClient {
  private client: MeiliSearch | null = null;
  private readonly MAX_RETRIES = 3;
  private readonly RETRY_DELAY = 1000; // ms

  constructor(
    private config: {
      host: string;
      apiKey?: string;
    },
  ) {
    this.initClient();
  }

  private initClient() {
    try {
      this.client = new MeiliSearch({
        host: this.config.host,
        apiKey: this.config.apiKey,
      });
    } catch (error) {
      this.client = null;
      console.error('Meilisearch client initialization error:', error);
    }
  }

  async ensureClient(): Promise<MeiliSearch> {
    if (!this.client) {
      this.initClient();
    }

    if (!this.client) {
      throw new Error('Failed to initialize Meilisearch client');
    }

    return this.client;
  }

  async suggest(
    indexName: string,
    query: string,
    options?: {
      limit?: number;
      attributesToSearchOn?: string[];
    },
  ): Promise<string[]> {
    return this.retryOperation(async () => {
      const client = await this.ensureClient();
      const index = client.index(indexName);

      const result = await index.search(query, {
        limit: options?.limit ?? 5,
        attributesToSearchOn: options?.attributesToSearchOn ?? ['name'],
        attributesToRetrieve: options?.attributesToSearchOn ?? ['name'],
      });

      return result.hits.map(hit =>
        options?.attributesToSearchOn?.[0] ? hit[options.attributesToSearchOn[0]] : hit['name'],
      );
    });
  }

  private async retryOperation<T>(
    operation: () => Promise<T>,
    retriesLeft = this.MAX_RETRIES,
  ): Promise<T> {
    try {
      return await operation();
    } catch (error) {
      if (retriesLeft === 0) throw error;

      await this.delay(this.RETRY_DELAY);
      return this.retryOperation(operation, retriesLeft - 1);
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  async health(): Promise<boolean> {
    try {
      const client = await this.ensureClient();
      await client.health();
      return true;
    } catch {
      return false;
    }
  }

  async updateApiKey(apiKey: string) {
    this.config.apiKey = apiKey;
    this.initClient();
  }

  // Ajouter ces méthodes
  public async getIndex(indexName: string) {
    const client = await this.ensureClient();
    return client.index(indexName).fetchInfo();
  }

  public async createIndex(indexName: string) {
    const client = await this.ensureClient();
    return client.createIndex(indexName);
  }

  public async updateIndexSettings(indexName: string, settings: any) {
    const client = await this.ensureClient();
    const index = client.index(indexName);
    return index.updateSettings(settings);
  }

  async search<T extends RecordAny = any>(
    indexName: string,
    query: string, // Le terme de recherche
    options?: {
      filter?: string | string[];
      limit?: number;
      offset?: number;
      attributesToRetrieve?: string[];
      facets?: string[];
      sort?: string[];
      lang?: string;
    },
  ): Promise<SearchResponse<T>> {
    return this.retryOperation(async () => {
      const client = await this.ensureClient();
      const index = client.index(indexName);

      // Créez un objet de paramètres de recherche
      const searchParams: Record<string, any> = {
        limit: options?.limit ?? 10,
        offset: options?.offset ?? 0,
        attributesToRetrieve: options?.attributesToRetrieve ?? [
          'id',
          'name',
          'title',
          'description',
        ],
      };

      // Ajoutez les filtres conditionnellement
      if (options?.filter) {
        searchParams['filter'] = options.filter;
      }

      if (options?.facets) {
        searchParams['facets'] = options.facets;
      }

      if (options?.sort) {
        searchParams['sort']  = options.sort;
      }

      // Filtrage par langue
      if (options?.lang) {
        searchParams['filter']  = searchParams['filter']
          ? `${searchParams['filter'] } AND lang = "${options.lang}"`
          : `lang = "${options.lang}"`;
      }

      // Effectuez la recherche avec le terme et les paramètres
      return await index.search<T>(query, searchParams);
    });
  }
}

export function createMeilisearchClient() {
  return new InstantSearchClient({
    host: environment.meiliHost,
    apiKey: environment.meiliSearchKey,
  });
}
