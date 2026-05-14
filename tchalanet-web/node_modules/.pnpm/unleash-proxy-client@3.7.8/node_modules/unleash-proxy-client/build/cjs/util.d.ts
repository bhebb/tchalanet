import { IContext } from '.';
export declare const notNullOrUndefined: ([, value]: [string, string]) => boolean;
export declare const urlWithContextAsQuery: (url: URL, context: IContext) => URL;
export declare const contextString: (context: IContext) => string;
export declare const computeContextHashValue: (obj: IContext) => Promise<string>;
export declare const parseHeaders: ({ clientKey, appName, connectionId, customHeaders, headerName, etag, isPost, }: {
    clientKey: string;
    connectionId: string;
    appName: string;
    customHeaders?: Record<string, string>;
    headerName?: string;
    etag?: string;
    isPost?: boolean;
}) => Record<string, string>;
//# sourceMappingURL=util.d.ts.map