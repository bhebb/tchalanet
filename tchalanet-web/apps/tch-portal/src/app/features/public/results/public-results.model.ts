export type ProviderKey = 'all' | 'ny' | 'fl' | 'ga' | 'tx';
export type SlotTypeKey = 'all' | 'mid' | 'eve' | 'late';
export const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;
export type PageSizeOption = (typeof PAGE_SIZE_OPTIONS)[number];
