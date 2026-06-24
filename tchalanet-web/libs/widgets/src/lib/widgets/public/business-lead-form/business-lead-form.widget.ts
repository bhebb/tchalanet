import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { isRecord, stringProp, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface FormFieldDef {
  id: string;
  type: 'text' | 'textarea';
  labelKey: string;
  placeholderKey: string;
  required: boolean;
}

type SendState = 'idle' | 'sending' | 'sent' | 'error';

@Component({
  selector: 'tch-public-business-lead-form-widget',
  imports: [LabelPipe, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './business-lead-form.widget.html',
  styleUrl: './business-lead-form.widget.scss',
})
export class PublicBusinessLeadFormWidget implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly sendState = signal<SendState>('idle');

  readonly sectionId = computed(() => stringProp(this.config(), 'id') ?? 'operator-demo-form');
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly subtitleKey = computed(() => stringProp(this.config(), 'subtitleKey'));
  readonly submitLabelKey = computed(() => stringProp(this.config(), 'submitLabelKey') ?? 'public.operator.form_cta');
  readonly sendingLabelKey = computed(() => stringProp(this.config(), 'sendingLabelKey') ?? 'common.sending');
  readonly sentLabelKey = computed(() => stringProp(this.config(), 'sentLabelKey') ?? 'public.operator.form_sent');

  readonly fields = computed<FormFieldDef[]>(() => {
    const raw = this.config().props?.['fields'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((f) => ({
      id: String(f['id'] ?? ''),
      type: f['type'] === 'textarea' ? 'textarea' : 'text',
      labelKey: String(f['labelKey'] ?? ''),
      placeholderKey: String(f['placeholderKey'] ?? ''),
      required: f['required'] === true,
    }));
  });

  readonly form = this.fb.nonNullable.group({} as Record<string, string>);

  ngOnInit(): void {
    this.fields().forEach((f) => {
      this.form.addControl(f.id, this.fb.nonNullable.control('', f.required ? Validators.required : []));
    });
  }

  submit(): void {
    if (this.form.invalid || this.sendState() === 'sending') return;

    const submitAction = this.config().props?.['submitAction'];
    const path = isRecord(submitAction) ? String(submitAction['path'] ?? '') : '/api/v1/public/operator-leads';

    this.sendState.set('sending');
    this.http.post(path, this.form.value).subscribe({
      next: () => this.sendState.set('sent'),
      error: () => this.sendState.set('error'),
    });
  }
}
