export type EntityHistoryType = 'SELLER_TERMINAL' | 'DRAW_RESULT' | 'LIMIT_ASSIGNMENT';

export type EntityRevisionOperation = 'CREATE' | 'UPDATE' | 'DELETE';

export interface EntityRevisionFieldChange {
  readonly field: string;
  readonly before?: string | null;
  readonly after?: string | null;
}

export interface EntityRevisionItem {
  readonly revisionId: string;
  readonly entityType: EntityHistoryType;
  readonly entityId: string;
  readonly operation: EntityRevisionOperation;
  readonly changedAt: string;
  readonly changedBy?: string | null;
  readonly tenantId?: string | null;
  readonly changedFields?: readonly string[];
  readonly changedValues?: readonly EntityRevisionFieldChange[];
}
