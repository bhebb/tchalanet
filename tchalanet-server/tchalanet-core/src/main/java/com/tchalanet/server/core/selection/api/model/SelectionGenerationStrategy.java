package com.tchalanet.server.core.selection.api.model;

/**
 * Strategy used to auto-generate a selection.
 * <p>
 * V1 supports {@link #RANDOM} only. {@link #LOW_EXPOSURE_RANDOM} is a reserved
 * value and must be rejected everywhere (effect validation, campaign
 * activation, generation, regeneration) until an exposure reader exists.
 * <p>
 * Source de vérité : {@code openspec/changes/maryaj-gratis-auto-selection-v1/}.
 */
public enum SelectionGenerationStrategy {
    RANDOM,
    LOW_EXPOSURE_RANDOM
}
