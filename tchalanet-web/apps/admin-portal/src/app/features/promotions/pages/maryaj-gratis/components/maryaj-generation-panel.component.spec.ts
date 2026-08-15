import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MaryajGenerationPanelComponent } from './maryaj-generation-panel.component';

describe(MaryajGenerationPanelComponent.name, () => {
  let formBuilder: FormBuilder;
  let component: MaryajGenerationPanelComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FormBuilder,
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    });
    formBuilder = TestBed.inject(FormBuilder);
    component = TestBed.runInInjectionContext(() => new MaryajGenerationPanelComponent());
    const testForm = form();
    (component as unknown as { form: () => typeof testForm }).form = () => testForm;
    (component as unknown as { editing: () => boolean }).editing = () => true;
  });

  it('reads generation state from the edit form', () => {
    expect(component.selectionLabel()).toBe('admin.maryajGratis.offer.selection.auto');
    expect(component.regenerationLabel()).toBe('admin.maryajGratis.generation.retryCount');
    expect(component.autoGenerationEnabled()).toBe(true);
    expect(component.manualSelectionEnabled()).toBe(false);
  });

  it('detects manual seller selection', () => {
    component.form().controls['choiceMode'].setValue('SELLER_SELECTS');

    expect(component.selectionLabel()).toBe('admin.maryajGratis.offer.selection.manual');
    expect(component.regenerationLabel()).toBe('admin.maryajGratis.generation.notUsed');
    expect(component.autoGenerationEnabled()).toBe(false);
    expect(component.manualSelectionEnabled()).toBe(true);
  });

  it('does not invent generation settings when read mode has no effect', () => {
    (component as unknown as { editing: () => boolean }).editing = () => false;
    (component as unknown as { effect: () => null }).effect = () => null;

    expect(component.configured()).toBe(false);
    expect(component.selectionLabel()).toBe('admin.maryajGratis.generation.notConfigured');
    expect(component.regenerationLabel()).toBe('admin.maryajGratis.generation.notConfigured');
  });

  it('emits manual selection changes from the generation panel', () => {
    const emit = vi.spyOn(component.manualSelectionChange, 'emit');

    component.manualSelectionChange.emit(true);

    expect(emit).toHaveBeenCalledWith(true);
  });

  function form() {
    return formBuilder.group({
      choiceMode: formBuilder.nonNullable.control<'AUTO_GENERATE' | 'SELLER_SELECTS'>(
        'AUTO_GENERATE',
      ),
      regenerableBeforeConfirm: formBuilder.nonNullable.control(true),
      maxRegenerationsBeforeConfirm: formBuilder.nonNullable.control(3),
    });
  }
});
