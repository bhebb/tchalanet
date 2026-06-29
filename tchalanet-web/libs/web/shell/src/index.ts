/**
 * Shared shell entrypoint.
 *
 * Reusable shell feedback, route-preserving actions, and layout primitives live
 * here when they are independent from one concrete app composition root.
 */
export * from './lib/feedback/copy-error-details';
export * from './lib/feedback/shell-feedback-banner.component';
export * from './lib/feedback/shell-feedback-outlet.component';
export * from './lib/feedback/shell-feedback.model';
export * from './lib/feedback/shell-feedback.store';
export * from './lib/private-shell/private-shell-layout.component';
export * from './lib/private-shell/private-navigation.model';
export * from './lib/public-shell/public-bottom-nav';
export * from './lib/public-shell/public-footer';
export * from './lib/public-shell/public-header';
export * from './lib/public-shell/public-shell-layout.component';
export * from './lib/public-shell/public-shell.service';
export * from './lib/public-shell/runtime/public-bootstrap.model';
export * from './lib/public-shell/runtime/public-bootstrap.service';
export * from './lib/public-shell/runtime/public-bootstrap.store';
export * from './lib/public-shell/runtime/public-fallback-bundle.service';
export * from './lib/public-shell/runtime/public-runtime.store';
export * from './lib/public-shell/runtime/public-runtime-initializer';
