import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
    selector: 'tch-error-panel',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <div class="error-panel" role="alert">
            <div class="error-panel__icon" aria-hidden="true">&#9888;</div>
            @if (title()) {
                <h3 class="error-panel__title">{{ title() }}</h3>
            }
            @if (message()) {
                <p class="error-panel__message">{{ message() }}</p>
            }
            @if (showRetry() && retryLabel()) {
                <button class="error-panel__retry" type="button" (click)="retry.emit()">
                    {{ retryLabel() }}
                </button>
            }
        </div>
    `,
    styles: [
        `
            .error-panel {
                display: grid;
                justify-items: center;
                gap: 0.75rem;
                padding: 2rem 1rem;
                text-align: center;
            }

            .error-panel__icon {
                font-size: 2rem;
                color: var(--tch-color-error, #ba1a1a);
            }

            .error-panel__title {
                margin: 0;
                font-size: var(--tch-font-size-title-md, 1.125rem);
                color: var(--tch-color-on-surface, #1a1c1e);
            }

            .error-panel__message {
                margin: 0;
                color: var(--tch-color-on-surface-variant, #464652);
                max-width: 34rem;
            }

            .error-panel__retry {
                min-height: var(--tch-touch-target, 48px);
                padding: 0 1.25rem;
                border-radius: var(--tch-radius-control, 8px);
                background: var(--tch-color-error-container, #ffdad6);
                color: var(--tch-color-on-error-container, #93000a);
                font-weight: 600;
                border: none;
                cursor: pointer;
            }
        `,
    ],
})
export class TchErrorPanel {
    readonly title = input('');
    readonly message = input('');
    readonly retryLabel = input('');
    readonly retry = output<void>();
    readonly showRetry = input(false);
}
