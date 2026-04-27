export type DecisionAttributeType = 'string' | 'number' | 'boolean' | 'date' | string;

export interface DecisionAttribute {
  name: string;
  type: DecisionAttributeType;
}

export interface DecisionEvent {
  type: string;
  params?: Record<string, unknown>;
}

export interface Decision {
  conditions: any; // json-rules-engine conditions
  event: DecisionEvent;
}

export interface DecisionSet {
  name: string;
  attributes?: DecisionAttribute[];
  decisions: Decision[];
}
